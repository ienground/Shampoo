package zone.ien.shampoo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.utils.Dlog

class BluetoothDeviceReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Dlog.d(TAG, "onReceive ${intent.getIntExtra(IntentKey.BLE_CHAR_CHANGED, -1)}")
    }
}