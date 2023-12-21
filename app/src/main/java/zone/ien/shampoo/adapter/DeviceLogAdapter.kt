package zone.ien.shampoo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DevModeActivity
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.databinding.AdapterDateHeaderBinding
import zone.ien.shampoo.databinding.AdapterDeviceLogBinding
import zone.ien.shampoo.room.DeviceLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceLogAdapter(var items: ArrayList<DeviceLogEntity>, var type: Int): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var context: Context
    private lateinit var timeFormat: SimpleDateFormat
    private lateinit var dateFormat: SimpleDateFormat

    private var callback: DevModeActivity.Callback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val binding: AdapterDeviceLogBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_device_log, parent, false)
        val headerBinding: AdapterDateHeaderBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_date_header, parent, false)

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
            holder.itemView.setOnClickListener {
                callback?.delete(items[holder.bindingAdapterPosition].id ?: -1)
            }
        } else if (holder is HeaderViewHolder) {
            if (holder.bindingAdapterPosition == 0) {
                holder.binding.divider.visibility = View.GONE
            }
            holder.binding.tvDate.text = dateFormat.format(Date(items[holder.bindingAdapterPosition].timestamp))
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position].capacity == -1 && items[position].battery == -1) CELL_TYPE_HEADER else CELL_TYPE_DEFAULT
    }

    fun setCallbackListener(clickCallback: DevModeActivity.Callback) {
        this.callback = clickCallback
    }

    inner class ItemViewHolder(val binding: AdapterDeviceLogBinding): RecyclerView.ViewHolder(binding.root)
    inner class HeaderViewHolder(val binding: AdapterDateHeaderBinding): RecyclerView.ViewHolder(binding.root)

    companion object {
        const val CELL_TYPE_HEADER = 0
        const val CELL_TYPE_DEFAULT = 1
    }
}