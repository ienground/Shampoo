package zone.ien.shampoo.callback

import android.bluetooth.BluetoothDevice

interface DeviceAddCallback {
    fun add(device: BluetoothDevice)
}