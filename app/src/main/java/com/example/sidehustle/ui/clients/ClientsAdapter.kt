package com.example.sidehustle.ui.clients

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sidehustle.R
import com.example.sidehustle.data.model.ClientResponse
import com.example.sidehustle.databinding.ItemClientBinding

/** Renders the client list (issue: Build Client Management). */
class ClientsAdapter(
    private val onClick: (ClientResponse) -> Unit,
) : ListAdapter<ClientResponse, ClientsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class ViewHolder(private val binding: ItemClientBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(client: ClientResponse, onClick: (ClientResponse) -> Unit) {
            binding.clientName.text = client.name
            binding.clientSubtitle.text = subtitleFor(client)
            binding.root.setOnClickListener { onClick(client) }
        }

        private fun subtitleFor(client: ClientResponse): String {
            val parts = listOfNotNull(
                client.email?.takeIf { it.isNotBlank() },
                client.phoneNumber?.takeIf { it.isNotBlank() },
            )
            return if (parts.isEmpty()) {
                binding.root.context.getString(R.string.clients_no_contact_info)
            } else {
                parts.joinToString(" · ")
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ClientResponse>() {
            override fun areItemsTheSame(oldItem: ClientResponse, newItem: ClientResponse) =
                oldItem.clientId == newItem.clientId

            override fun areContentsTheSame(oldItem: ClientResponse, newItem: ClientResponse) =
                oldItem == newItem
        }
    }
}
