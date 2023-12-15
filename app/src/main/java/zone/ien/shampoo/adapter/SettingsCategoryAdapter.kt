package zone.ien.shampoo.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.callback.PreferenceCallback
import zone.ien.shampoo.databinding.AdapterSettingsCategoryBinding
import zone.ien.shampoo.data.CategoryObject
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors

class SettingsCategoryAdapter(val items: ArrayList<CategoryObject>): RecyclerView.Adapter<SettingsCategoryAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var selectedId = 0
    private var callbackListener: PreferenceCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val binding: AdapterSettingsCategoryBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_settings_category, parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[holder.bindingAdapterPosition])
        val root = holder.binding.root as ConstraintLayout
        if (items[holder.bindingAdapterPosition].id == selectedId && context.resources.getBoolean(R.bool.is_w600dp)) {
            root.backgroundTintList = ColorStateList.valueOf(getAttrColor(context.theme, Colors.colorSurfaceVariant))
            holder.binding.icBackground.backgroundTintList = ColorStateList.valueOf(getAttrColor(context.theme, Colors.colorSurface))
        } else {
            root.backgroundTintList = ColorStateList.valueOf(getAttrColor(context.theme, Colors.colorSurface))
            holder.binding.icBackground.backgroundTintList = ColorStateList.valueOf(getAttrColor(context.theme, Colors.colorPrimaryContainer))
        }
        root.setOnClickListener {
            val preSelectedPosition = items.indexOfFirst { it.id == selectedId }
            selectedId = items[holder.bindingAdapterPosition].id
            notifyItemChanged(preSelectedPosition)
            notifyItemChanged(holder.bindingAdapterPosition)
            callbackListener?.click(items[holder.bindingAdapterPosition].id)
        }
        holder.binding.tvContent.isSelected = true
    }

    fun setSelectedId(id: Int) {
        selectedId = id
    }

    fun getSelectedId() = selectedId

    override fun getItemCount(): Int = items.size

    fun setCallbackListener(callbackListener: PreferenceCallback) {
        this.callbackListener = callbackListener
    }

    fun add(item: CategoryObject, position: Int? = null) {
        if (position != null) {
            items.add(position, item)
            notifyItemInserted(position)
        } else {
            items.add(item)
            notifyItemInserted(items.lastIndex)
        }
    }

    fun remove(id: Int) {
        val index = items.indexOfFirst { it.id == id }
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    fun select(id: Int) {
        val prePosition = items.indexOfFirst { it.id == selectedId }
        val position = items.indexOfFirst { it.id == id }
        selectedId = id
        callbackListener?.click(id)
        notifyItemChanged(prePosition)
        notifyItemChanged(position)
    }

    inner class ItemViewHolder(val binding: AdapterSettingsCategoryBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryObject) {
            binding.item = item
        }
    }
}