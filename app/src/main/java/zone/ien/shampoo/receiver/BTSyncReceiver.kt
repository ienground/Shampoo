package zone.ien.shampoo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.utils.Dlog
import java.util.Calendar

class BTSyncReceiver: BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        Dlog.d(TAG, "BTSyncReceiver")
        val deviceDatabase = DeviceDatabase.getInstance(context)

        GlobalScope.launch(Dispatchers.IO) {
            val devices = deviceDatabase?.getDao()?.getAll()
            devices?.forEach {
                val calendar = Calendar.getInstance()

                context.sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                    putExtra(IntentKey.DEVICE_ADDRESS, it.address)
                    putExtra(IntentKey.MESSAGE_TYPE, if (calendar[Calendar.MINUTE] % 2 == 0) MessageType.TYPE_BATTERY else MessageType.TYPE_LEVEL)
                    putExtra(IntentKey.MESSAGE_VALUE, "Request")
                })
            }
        }
    }
}