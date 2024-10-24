package com.travis.inshape

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: MutableList<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        if (message.isUser) {
            holder.userMessage.text = message.text
            holder.userMessage.visibility = View.VISIBLE
            holder.chatbotMessage.visibility = View.GONE
        } else {
            holder.chatbotMessage.text = message.text
            holder.chatbotMessage.visibility = View.VISIBLE
            holder.userMessage.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userMessage: TextView = itemView.findViewById(R.id.user_message)
        val chatbotMessage: TextView = itemView.findViewById(R.id.chatbot_message)
    }

    // Remove the last message from the list
    fun removeLastMessage() {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            messages.removeAt(lastIndex)
            notifyItemRemoved(lastIndex)
        }
    }
}
