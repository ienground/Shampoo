package zone.ien.shampoo.adapter

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.callback.DeviceAddCallback
import zone.ien.shampoo.databinding.AdapterDeviceAddScanBinding
import zone.ien.shampoo.utils.Dlog

class DeviceAddScanAdapter(private var items: ArrayList<BluetoothDevice>): RecyclerView.Adapter<DeviceAddScanAdapter.ItemViewHolder>() {

    private lateinit var context: Context
    private var callbackListener: DeviceAddCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val binding: AdapterDeviceAddScanBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.adapter_device_add_scan, parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            holder.binding.tvDeviceName.text = items[holder.bindingAdapterPosition].name
            holder.binding.tvDeviceType.text = items[holder.bindingAdapterPosition].address
        }

        holder.binding.btnAdd.setOnClickListener {
            callbackListener?.add(items[holder.bindingAdapterPosition])
        }
    }

    fun add(device: BluetoothDevice) {
        items.add(device)
        notifyItemInserted(itemCount - 1)
    }

    fun setClickCallback(callbackListener: DeviceAddCallback) {
        this.callbackListener = callbackListener
    }

    inner class ItemViewHolder(val binding: AdapterDeviceAddScanBinding): RecyclerView.ViewHolder(binding.root)
}