package com.example.sidehustle.ui.projects

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sidehustle.R
import com.example.sidehustle.data.model.JobResponse
import com.example.sidehustle.databinding.ItemJobBinding

/** Renders the job list — title, client, status, due date (issue: Build Job Management). */
class JobsAdapter(
    private val onClick: (JobResponse) -> Unit,
) : ListAdapter<JobResponse, JobsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class ViewHolder(private val binding: ItemJobBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(job: JobResponse, onClick: (JobResponse) -> Unit) {
            val context = binding.root.context

            binding.jobTitle.text = job.title
            binding.jobClient.text = job.clientName
            binding.jobDueDate.text = job.dueDate?.takeIf { it.isNotBlank() }
                ?.let { context.getString(R.string.jobs_due_date_label, it) }
                ?: context.getString(R.string.jobs_no_due_date)

            binding.jobStatusBadge.text = job.status
            binding.jobStatusBadge.backgroundTintList =
                ContextCompat.getColorStateList(context, colorForStatus(job.status))

            binding.root.setOnClickListener { onClick(job) }
        }

        private fun colorForStatus(status: String): Int = when (status) {
            "In Progress" -> R.color.status_in_progress
            "Completed" -> R.color.status_completed
            "Cancelled" -> R.color.status_cancelled
            else -> R.color.status_pending
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<JobResponse>() {
            override fun areItemsTheSame(oldItem: JobResponse, newItem: JobResponse) =
                oldItem.jobId == newItem.jobId

            override fun areContentsTheSame(oldItem: JobResponse, newItem: JobResponse) =
                oldItem == newItem
        }
    }
}
