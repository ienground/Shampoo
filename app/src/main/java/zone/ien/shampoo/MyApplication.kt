package zone.ien.shampoo

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.color.DynamicColors
import zone.ien.shampoo.constant.SharedDefault
import zone.ien.shampoo.constant.SharedKey

class MyApplication: Application(), Application.ActivityLifecycleCallbacks {

    companion object {
        private lateinit var sharedPreferences: SharedPreferences
        private var sharedPreferencesEditor: SharedPreferences.Editor? = null
    }

    private var currentActivity: Activity? = null
    private var loadTime: Long = 0

    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Updating the currentActivity only when an ad is not showing.
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean(SharedKey.MATERIAL_YOU, SharedDefault.MATERIAL_YOU)) DynamicColors.applyToActivitiesIfAvailable(this)

        registerActivityLifecycleCallbacks(this)
    }

}