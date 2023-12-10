package zone.ien.shampoo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.databinding.AdapterDeviceLogBinding
import zone.ien.shampoo.databinding.AdapterDeviceLogHeaderBinding
import zone.ien.shampoo.room.DeviceLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceLogAdapter(var items: ArrayList<DeviceLogEntity>, var type: Int): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var context: Context
    private lateinit var timeFormat: SimpleDateFormat
    private lateinit var dateFormat: SimpleDateFormat

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val binding: AdapterDeviceLogBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_device_log, parent, false)
        val headerBinding: AdapterDeviceLogHeaderBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_device_log_header, parent, false)

        timeFormat = SimpleDateFormat(context.getString(R.string.timeFormat), Locale.getDefault())
        dateFormat = SimpleDateFormat(context.getString(R.string.dateFormat), Locale.getDefault())

        return if (viewType == CELL_TYPE_HEADER) HeaderViewHolder(headerBinding) else ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.binding.tvTime.text = timeFormat.format(Date(items[holder.bindingAdapterPosition].timestamp))
            holder.binding.tvValue.text = when (type) {
                MessageType.TYPE_BATTERY -> items[holder.bindingAdapterPosition].battery
                MessageType.TYPE_LEVEL -> items[holder.bindingAdapterPosition].capacity
                else -> ""
            }.toString()
        } else if (holder is HeaderViewHolder) {
            holder.binding.tvDate.text = dateFormat.format(Date(items[holder.bindingAdapterPosition].timestamp))
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position].capacity == -1 && items[position].battery == -1) CELL_TYPE_HEADER else CELL_TYPE_DEFAULT
    }

    inner class ItemViewHolder(val binding: AdapterDeviceLogBinding): RecyclerView.ViewHolder(binding.root)
    inner class HeaderViewHolder(val binding: AdapterDeviceLogHeaderBinding): RecyclerView.ViewHolder(binding.root)

    companion object {
        const val CELL_TYPE_HEADER = 0
        const val CELL_TYPE_DEFAULT = 1
    }
}