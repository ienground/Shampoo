package zone.ien.shampoo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.databinding.AdapterDateHeaderBinding
import zone.ien.shampoo.databinding.AdapterNotificationsBinding
import zone.ien.shampoo.room.NotificationsEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(var items: ArrayList<NotificationsEntity>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var context: Context
    private lateinit var dateFormat: SimpleDateFormat
    private lateinit var timeFormat: SimpleDateFormat

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val binding: AdapterNotificationsBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_notifications, parent, false)
        val dateBinding: AdapterDateHeaderBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_date_header, parent, false)

        dateFormat = SimpleDateFormat(context.getString(R.string.dateFormat), Locale.getDefault())
        timeFormat = SimpleDateFormat(context.getString(R.string.timeFormat), Locale.getDefault())

        return if (viewType == CELL_TYPE_HEADER) DateItemViewHolder(dateBinding) else ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.binding.tvDate.text = timeFormat.format(Date(items[holder.bindingAdapterPosition].timestamp))
            holder.binding.tvTitle.text = items[holder.bindingAdapterPosition].title
            holder.binding.tvContent.text = items[holder.bindingAdapterPosition].content
            holder.binding.icIcon.setImageResource(when (items[holder.bindingAdapterPosition].type) {
                MessageType.TYPE_LEVEL_LOW -> R.drawable.ic_shampoo_alert
                MessageType.TYPE_BATTERY_LOW -> R.drawable.ic_battery_alert
                else -> R.drawable.ic_qmark
            })
        } else if (holder is DateItemViewHolder) {
            holder.binding.tvDate.text = dateFormat.format(Date(items[holder.bindingAdapterPosition].timestamp))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].type == -1) CELL_TYPE_HEADER else CELL_TYPE_DEFAULT
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(val binding: AdapterNotificationsBinding): RecyclerView.ViewHolder(binding.root)
    inner class DateItemViewHolder(val binding: AdapterDateHeaderBinding): RecyclerView.ViewHolder(binding.root)

    companion object {
        const val CELL_TYPE_HEADER = 0
        const val CELL_TYPE_DEFAULT = 1
    }
}