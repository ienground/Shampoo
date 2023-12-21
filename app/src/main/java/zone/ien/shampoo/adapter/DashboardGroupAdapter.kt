package zone.ien.shampoo.adapter

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.callback.DashboardCallback
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.databinding.AdapterDashboardBinding
import zone.ien.shampoo.databinding.AdapterDashboardGroupBinding
import zone.ien.shampoo.room.PlaceDatabase
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils.getBatteryDrawable

class DashboardGroupAdapter(var items: ArrayList<Pair<Long, DashboardAdapter>>): RecyclerView.Adapter<DashboardGroupAdapter.ItemViewHolder>() {

    private lateinit var context: Context
    private var callbackListener: DashboardCallback? = null
    private var placeDatabase: PlaceDatabase? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        placeDatabase = PlaceDatabase.getInstance(context)
        val binding: AdapterDashboardGroupBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_dashboard_group, parent, false)
        return ItemViewHolder(binding)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        callbackListener?.let { items[holder.bindingAdapterPosition].second.setClickCallback(it) }
        holder.binding.list.adapter = items[holder.bindingAdapterPosition].second

        GlobalScope.launch(Dispatchers.IO) {
            val place = placeDatabase?.getDao()?.get(items[holder.bindingAdapterPosition].first)
            Dlog.d(TAG, "$place ${items[holder.bindingAdapterPosition].first}")
            place?.let {
                holder.binding.tvIcon.text = it.icon
                holder.binding.tvPlace.text = it.title
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun setClickCallback(callbackListener: DashboardCallback) {
        this.callbackListener = callbackListener
    }

    fun add(entity: DeviceEntity) {
        val index = items.indexOfFirst { it.first == entity.room }
        if (index != -1) {
            items[index].second.add(entity)
            notifyItemChanged(index)
        } else {
            items.add(Pair(entity.room, DashboardAdapter(arrayListOf(entity))))
//            items.add(Pair(entity.room, arrayListOf(entity)))
            notifyItemInserted(items.lastIndex)
        }
    }

    fun update(entity: DeviceEntity) {
        val index = items.indexOfFirst { it.first == entity.room }
        if (index != -1) {
            items[index].second.update(entity)
            notifyItemChanged(index)
        }
    }

    fun updateConnectionState(address: String, isConnected: Boolean) {
        var index = -1
        for ((i, item) in items.withIndex()) {
            val subIndex = item.second.items.indexOfFirst { it.address == address }
            if (subIndex != -1) {
                index = i
                item.second.updateConnectionState(address, isConnected)
                break
            }
        }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    fun delete(id: Long) {
        var index = -1
        for ((i, item) in items.withIndex()) {
            val subIndex = item.second.items.indexOfFirst { it.id == id }
            if (subIndex != -1) {
                index = i
                item.second.delete(id)
                break
            }
        }
        if (index != -1) {
            if (items[index].second.itemCount != 0) {
                notifyItemChanged(index)
            } else {
                items.removeAt(index)
                notifyItemRemoved(index)
            }
        }
    }

    inner class ItemViewHolder(val binding: AdapterDashboardGroupBinding): RecyclerView.ViewHolder(binding.root) {

    }
}