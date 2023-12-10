package zone.ien.shampoo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.callback.DashboardCallback
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.databinding.AdapterDashboardBinding
import zone.ien.shampoo.utils.Dlog

class DashboardAdapter(var items: ArrayList<DeviceEntity>): RecyclerView.Adapter<DashboardAdapter.ItemViewHolder>() {

    private lateinit var context: Context
    private var callbackListener: DashboardCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val binding: AdapterDashboardBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_dashboard, parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.binding.tvCapacity.text = "${(items[holder.bindingAdapterPosition].capacity.toFloat() / items[holder.bindingAdapterPosition].max * 100).toInt()}%"
        holder.binding.progress.max = items[holder.bindingAdapterPosition].max.toFloat()
        holder.binding.progress.progress = items[holder.bindingAdapterPosition].capacity.toFloat()
        holder.binding.tvName.text = items[holder.bindingAdapterPosition].title
        holder.binding.tvType.text = context.getString(when (items[holder.bindingAdapterPosition].type) {
            DeviceEntity.TYPE_SHAMPOO -> R.string.shampoo
            DeviceEntity.TYPE_CONDITIONER -> R.string.conditioner
            DeviceEntity.TYPE_BODYWASH -> R.string.bodywash
            DeviceEntity.TYPE_CLEANSING -> R.string.cleansing
            else -> R.string.unknown
        })
        holder.binding.icBattery.setImageResource(when (items[holder.bindingAdapterPosition].battery) {
            in 95..100 -> R.drawable.ic_battery_full
            in 90 until 95 -> R.drawable.ic_battery_90
            in 70 until 90 -> R.drawable.ic_battery_80
            in 60 until 70 -> R.drawable.ic_battery_60
            in 50 until 60 -> R.drawable.ic_battery_50
            in 30 until 50 -> R.drawable.ic_battery_30
            in 20 until 30 -> R.drawable.ic_battery_20
            else -> R.drawable.ic_battery_alert
        })
        holder.binding.root.setOnClickListener {
            callbackListener?.callback(holder.bindingAdapterPosition, items[holder.bindingAdapterPosition].id ?: -1)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setClickCallback(callbackListener: DashboardCallback) {
        this.callbackListener = callbackListener
    }

    fun add(entity: DeviceEntity) {
        items.add(entity)
        notifyItemInserted(items.lastIndex)
//        items.sortBy { it.room }
    }

    fun delete(id: Long) {
        val index = items.indexOfFirst { it.id == id }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }

    }

    inner class ItemViewHolder(val binding: AdapterDashboardBinding): RecyclerView.ViewHolder(binding.root) {

    }
}