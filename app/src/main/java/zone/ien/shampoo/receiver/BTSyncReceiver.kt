package zone.ien.shampoo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.shampoo.room.DeviceDatabase

class BTSyncReceiver: BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val deviceDatabase = DeviceDatabase.getInstance(context)

        GlobalScope.launch(Dispatchers.IO) {
            val devices = deviceDatabase?.getDao()?.getAll()
            devices?.forEach {

            }
        }
    }
}