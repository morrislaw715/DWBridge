package com.nx.dwbridge.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nx.dwbridge.R
import com.nx.dwbridge.ws.WsMessage
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter : ListAdapter<WsMessage, MessagesAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<WsMessage>() {
            override fun areItemsTheSame(oldItem: WsMessage, newItem: WsMessage): Boolean = oldItem.timestamp == newItem.timestamp
            override fun areContentsTheSame(oldItem: WsMessage, newItem: WsMessage): Boolean = oldItem == newItem
        }
        val TIME_FMT = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMain: TextView = itemView.findViewById(R.id.tv_main)
        private val tvSub: TextView = itemView.findViewById(R.id.tv_sub)

        fun bind(m: WsMessage) {
            tvMain.text = m.data
            tvSub.text = "${TIME_FMT.format(Date(m.timestamp))} ${m.symbology ?: ""}"
        }
    }
}

