package zone.ien.shampoo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.databinding.AdapterNotificationsBinding
import zone.ien.shampoo.room.NotificationsEntity
import java.util.Date

class NotificationsAdapter(var items: ArrayList<NotificationsEntity>): RecyclerView.Adapter<NotificationsAdapter.ItemViewHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val binding: AdapterNotificationsBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_notifications, parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.binding.tvDate.text = Date(items[holder.bindingAdapterPosition].timestamp).toString()
        holder.binding.tvTitle.text = items[holder.bindingAdapterPosition].title
    }

    override fun getItemCount(): Int = items.size
    inner class ItemViewHolder(val binding: AdapterNotificationsBinding): RecyclerView.ViewHolder(binding.root)
}