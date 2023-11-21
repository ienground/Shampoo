package zone.ien.shampoo.service

import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.os.IBinder

class DeviceService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {


        return super.onStartCommand(intent, flags, startId)
    }


}