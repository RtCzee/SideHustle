package com.example.sidehustle.ui.attachments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.sidehustle.R
import com.example.sidehustle.SideHustleApp
import com.example.sidehustle.data.model.AttachmentRequest
import com.example.sidehustle.data.remote.ApiResult
import com.example.sidehustle.data.remote.RemoteDataSource
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Upload only to an already saved entity; failed API saves retain the uploaded URL for retry. */
class AttachmentsDialog : DialogFragment() {
    private val remote = RemoteDataSource()
    private val type get() = arguments?.getString(ARG_TYPE).orEmpty()
    private val entityId get() = arguments?.getString(ARG_ID).orEmpty()
    private var status: TextView? = null
    private var files: LinearLayout? = null
    private var pick: MaterialButton? = null
    private var retry: MaterialButton? = null
    private var close: MaterialButton? = null
    private var pending: AttachmentRequest? = null
    private var busy = false

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (view == null) return@registerForActivityResult
        if (uri == null) message(R.string.attachment_cancelled) else upload(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = savedInstanceState?.getString("pendingName")
        val url = savedInstanceState?.getString("pendingUrl")
        val path = savedInstanceState?.getString("pendingPath")
        if (name != null && url != null && path != null) pending = AttachmentRequest(name, url, path)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pending?.let {
            outState.putString("pendingName", it.name)
            outState.putString("pendingUrl", it.url)
            outState.putString("pendingPath", it.storagePath)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(TextView(context).apply {
                text = arguments?.getString(ARG_SUMMARY).orEmpty()
                textSize = 20f
            })
            addView(TextView(context).apply { setText(R.string.attachments_title) })
            status = TextView(context).also { addView(it) }
            files = LinearLayout(context).also { it.orientation = LinearLayout.VERTICAL; addView(it) }
            pick = MaterialButton(context).also {
                it.setText(R.string.attachment_add)
                it.setOnClickListener {
                    try { picker.launch(arrayOf("*/*")) }
                    catch (_: Exception) { message(R.string.attachment_picker_failed) }
                }
                addView(it)
            }
            retry = MaterialButton(context).also {
                it.setText(R.string.attachment_retry)
                it.setOnClickListener { if (pending != null) savePending() else load() }
                addView(it)
            }
            close = MaterialButton(context).apply {
                setText(R.string.attachment_close)
                setOnClickListener { dismiss() }
            }.also { addView(it) }
        }.let { content ->
            android.widget.ScrollView(context).apply { addView(content) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (type !in supportedEntityTypes || entityId.isBlank()) {
            message(R.string.error_generic_message)
            pick?.isEnabled = false
            retry?.isEnabled = false
            return
        }
        load()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun message(resource: Int) { status?.setText(resource) }

    private fun setBusy(value: Boolean) {
        busy = value
        pick?.isEnabled = !value && pending == null
        retry?.isEnabled = !value
        close?.isEnabled = !value
        isCancelable = !value
    }

    private fun load() {
        if (busy) return
        setBusy(true)
        message(R.string.attachment_loading)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = requireActivity().application as SideHustleApp
                when (val result = app.fetchAttachments(type, entityId)) {
                    is ApiResult.Success -> {
                        files?.removeAllViews()
                        result.data.forEach { attachment ->
                            files?.addView(MaterialButton(requireContext()).apply {
                                text = getString(R.string.attachment_open, attachment.name)
                                setOnClickListener {
                                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url))) }
                                    catch (_: ActivityNotFoundException) { message(R.string.attachment_open_failed) }
                                    catch (_: SecurityException) { message(R.string.attachment_open_failed) }
                                }
                            })
                        }
                        message(if (pending != null) R.string.attachment_save_failed else if (result.data.isEmpty())
                            R.string.attachment_empty else R.string.attachment_ready)
                    }
                    is ApiResult.Error -> message(R.string.attachment_load_failed)
                }
            } finally { setBusy(false) }
        }
    }

    private fun upload(uri: Uri) {
        if (busy) return
        setBusy(true)
        message(R.string.attachment_uploading)
        viewLifecycleOwner.lifecycleScope.launch {
            var saved = false
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Sign in required")
                val resolver = requireContext().contentResolver
                var name = getString(R.string.attachment_default_name)
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) name = it.getString(nameIndex)?.take(255)?.ifBlank { name } ?: name
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex >= 0 && !it.isNull(sizeIndex) && it.getLong(sizeIndex) > 20 * 1024 * 1024) {
                            message(R.string.attachment_too_large)
                            return@launch
                        }
                    }
                }
                val ref = FirebaseStorage.getInstance().reference.child("attachments/$uid/$type/$entityId/${UUID.randomUUID()}")
                val metadata = StorageMetadata.Builder()
                    .setContentType(resolver.getType(uri) ?: "application/octet-stream").build()
                val task = ref.putFile(uri, metadata)
                try { task.await() } catch (cancelled: CancellationException) { task.cancel(); throw cancelled }
                pending = AttachmentRequest(name, ref.downloadUrl.await().toString(), ref.path.removePrefix("/"))
                persistPending()
                saved = pending == null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message(R.string.attachment_upload_failed) }
            finally { setBusy(false) }
            if (saved) load()
        }
    }

    private fun savePending() {
        if (busy) return
        setBusy(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try { persistPending() } finally { setBusy(false) }
            if (pending == null) load()
        }
    }

    private suspend fun persistPending() {
        val request = pending ?: return
        when (remote.addAttachment(type, entityId, request)) {
            is ApiResult.Success -> { pending = null; message(R.string.attachment_saved) }
            is ApiResult.Error -> message(R.string.attachment_save_failed)
        }
    }

    override fun onDestroyView() {
        status = null; files = null; pick = null; retry = null; close = null; busy = false
        super.onDestroyView()
    }

    companion object {
        fun show(manager: FragmentManager, type: String, id: String, summary: String) {
            if (type !in supportedEntityTypes || id.isBlank()) return
            if (!manager.isStateSaved && manager.findFragmentByTag("attachments") == null) {
                AttachmentsDialog().apply {
                    arguments = bundleOf(ARG_TYPE to type, ARG_ID to id, ARG_SUMMARY to summary)
                }.show(manager, "attachments")
            }
        }

        private const val ARG_TYPE = "type"
        private const val ARG_ID = "id"
        private const val ARG_SUMMARY = "summary"
        private val supportedEntityTypes = setOf("jobs", "expenses")
    }
}
