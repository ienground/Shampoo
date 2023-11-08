package zone.ien.shampoo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.data.DashboardEntity
import zone.ien.shampoo.databinding.AdapterDashboardBinding

class DashboardAdapter(var items: ArrayList<DashboardEntity>): RecyclerView.Adapter<DashboardAdapter.ItemViewHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val binding: AdapterDashboardBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_dashboard, parent, false);
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.binding.tvCapacity.text = "${items[holder.bindingAdapterPosition].capacity}%"
        holder.binding.progress.max = 100f
        holder.binding.progress.progress = items[holder.bindingAdapterPosition].capacity.toFloat()
        holder.binding.tvName.text = items[holder.bindingAdapterPosition].title
        holder.binding.tvType.text = context.getString(when (items[holder.bindingAdapterPosition].type) {
            DashboardEntity.TYPE_SHAMPOO -> R.string.shampoo
            DashboardEntity.TYPE_CONDITIONER -> R.string.conditioner
            DashboardEntity.TYPE_BODYWASH -> R.string.bodywash
            DashboardEntity.TYPE_CLEANSING -> R.string.cleansing
            else -> R.string.unknown
        })
    }

    override fun getItemCount(): Int = items.size



    inner class ItemViewHolder(val binding: AdapterDashboardBinding): RecyclerView.ViewHolder(binding.root) {

    }
}