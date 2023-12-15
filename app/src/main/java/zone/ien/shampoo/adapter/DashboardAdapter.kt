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
import zone.ien.shampoo.utils.MyUtils.getBatteryDrawable

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
        holder.binding.icBattery.setImageResource(getBatteryDrawable(items[holder.bindingAdapterPosition].battery))
        holder.binding.root.setOnClickListener {
            callbackListener?.callback(holder.bindingAdapterPosition, items[holder.bindingAdapterPosition].id ?: -1)
        }
        holder.binding.icConnect.setImageResource(if (items[holder.bindingAdapterPosition].isConnected) R.drawable.ic_cloud else R.drawable.ic_cloud_off)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            for (payload in payloads) {
                if (payload is String) {
                    holder.binding.icConnect.setImageResource(if (items[holder.bindingAdapterPosition].isConnected) R.drawable.ic_cloud else R.drawable.ic_cloud_off)
                }
            }
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

    fun update(entity: DeviceEntity) {
        val index = items.indexOfFirst { it.id == entity.id }
        if (index != -1) {
            items[index] = entity
            notifyItemChanged(index)
        }
    }

    fun updateConnectionState(address: String, isConnected: Boolean) {
        val index = items.indexOfFirst { it.address == address }
        if (index != -1) {
            items[index].isConnected = isConnected
            notifyItemChanged(index, "")
        }
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