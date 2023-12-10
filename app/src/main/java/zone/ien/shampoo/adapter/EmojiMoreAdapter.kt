package zone.ien.shampoo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.callback.EmojiClickCallback
import zone.ien.shampoo.databinding.AdapterEmojiMoreBinding

class EmojiMoreAdapter(var items: Array<String>): RecyclerView.Adapter<EmojiMoreAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var callbackListener: EmojiClickCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val binding: AdapterEmojiMoreBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_emoji_more, parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[holder.absoluteAdapterPosition])
        holder.binding.tv.text = items[holder.absoluteAdapterPosition]
        holder.binding.tv.setOnClickListener {
            callbackListener?.click(holder.binding.tv.text.toString())
        }
    }

    fun setCallbackListener(callbackListener: EmojiClickCallback) {
        this.callbackListener = callbackListener
    }

    inner class ItemViewHolder(val binding: AdapterEmojiMoreBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.item = item
        }
    }
}