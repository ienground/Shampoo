package zone.ien.shampoo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.databinding.AdapterDeviceLogBinding
import zone.ien.shampoo.room.DeviceLogEntity
import java.util.Date

class DeviceLogAdapter(var items: ArrayList<DeviceLogEntity>, var type: Int): RecyclerView.Adapter<DeviceLogAdapter.ItemViewHolder>() {

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val binding: AdapterDeviceLogBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_device_log, parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.binding.tvTime.text = Date(items[holder.bindingAdapterPosition].timestamp).toString()
        holder.binding.tvValue.text = when (type) {
            MessageType.TYPE_BATTERY -> items[holder.bindingAdapterPosition].battery
            MessageType.TYPE_LEVEL -> items[holder.bindingAdapterPosition].capacity
            else -> ""
        }.toString()
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(val binding: AdapterDeviceLogBinding): RecyclerView.ViewHolder(binding.root)
}