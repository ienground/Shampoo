package zone.ien.shampoo.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.constant.PendingIntentReqCode
import zone.ien.shampoo.utils.Dlog
import java.util.Calendar

class BootDeviceReceiver : BroadcastReceiver() {

    /**
     * https://www.dev2qa.com/how-to-start-android-service-automatically-at-boot-time/
     */

    lateinit var am: AlarmManager
    lateinit var sharedPreferences: SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        Dlog.d(TAG, "BootDeviceReceiver onReceive, action is $action ${intent}")

        am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        sharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                }
                val pendingIntent = PendingIntent.getBroadcast(context, PendingIntentReqCode.BUBBLE_SCHEDULE_DAY, Intent(context, BTSyncReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_HOUR, pendingIntent)
            }
        }
    }

}