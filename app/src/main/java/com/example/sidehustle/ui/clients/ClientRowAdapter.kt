package com.example.sidehustle.ui.clients

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sidehustle.R
import com.example.sidehustle.databinding.ItemClientRowBinding

/**
 * Placeholder client model. Replaced by the real client model when client management (#14) lands.
 */
data class DemoClient(
    val id: Long,
    val name: String,
    val updatedAt: String,
)

class ClientRowAdapter(
    private val onClientClicked: (DemoClient) -> Unit,
) : ListAdapter<DemoClient, ClientRowAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClientRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClientClicked)
    }

    class ViewHolder(private val binding: ItemClientRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(client: DemoClient, onClicked: (DemoClient) -> Unit) {
            binding.clientInitial.text = client.name.take(1).uppercase()
            binding.clientName.text = client.name
            binding.clientSubtitle.text =
                binding.root.context.getString(R.string.client_row_updated, client.updatedAt)
            binding.root.setOnClickListener { onClicked(client) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DemoClient>() {
        override fun areItemsTheSame(oldItem: DemoClient, newItem: DemoClient) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DemoClient, newItem: DemoClient) = oldItem == newItem
    }
}
