package zone.ien.shampoo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.callback.PlaceCallback
import zone.ien.shampoo.databinding.AdapterPlaceBinding
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.room.PlaceEntity

class PlaceAdapter(var items: ArrayList<PlaceEntity>): RecyclerView.Adapter<PlaceAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var callbackListener: PlaceCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val binding: AdapterPlaceBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_place, parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[holder.bindingAdapterPosition])
        holder.binding.btnEdit.setOnClickListener {
            callbackListener?.edit(holder.bindingAdapterPosition, items[holder.bindingAdapterPosition])
        }
        holder.binding.btnDelete.setOnClickListener {
            callbackListener?.delete(holder.bindingAdapterPosition, items[holder.bindingAdapterPosition].id ?: -1)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setClickCallback(callbackListener: PlaceCallback) {
        this.callbackListener = callbackListener
    }

    fun add(entity: PlaceEntity) {
        items.add(entity)
        notifyItemInserted(items.lastIndex)
    }

    fun edit(entity: PlaceEntity) {
        val index = items.indexOfFirst { it.id == entity.id }
        if (index != -1) {
            items[index].icon = entity.icon
            items[index].title = entity.title
            notifyItemChanged(index)
        }
    }

    fun delete(id: Long) {
        val index = items.indexOfFirst { it.id == id }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }

    }


    inner class ItemViewHolder(val binding: AdapterPlaceBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(entity: PlaceEntity) {
            binding.entity = entity
        }
    }
}