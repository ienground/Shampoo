package zone.ien.shampoo.utils

import android.util.Log
import zone.ien.shampoo.BuildConfig

object Dlog {
    const val TAG = "TedPark"

    /**
     * Log Level Error
     */
    fun e(tag: String, message: String?) {
        if (BuildConfig.DEBUG) Log.e(tag, buildLogMsg(message))
    }

    /**
     * Log Level Warning
     */
    fun w(tag: String, message: String?) {
        if (BuildConfig.DEBUG) Log.w(tag, buildLogMsg(message))
    }

    /**
     * Log Level Information
     */
    fun i(tag: String, message: String?) {
        if (BuildConfig.DEBUG) Log.i(tag, buildLogMsg(message))
    }

    /**
     * Log Level Debug
     */
    fun d(tag: String, message: String?) {
        if (BuildConfig.DEBUG) Log.d(tag, buildLogMsg(message))
    }

    /**
     * Log Level Verbose
     */
    fun v(tag: String, message: String?) {
        if (BuildConfig.DEBUG) Log.v(tag, buildLogMsg(message))
    }

    private fun buildLogMsg(message: String?): String {
        val ste = Thread.currentThread().stackTrace[4]
        val sb = StringBuilder()
        sb.append("[")
        sb.append(ste.fileName.replace(".kt", ""))
        sb.append("::")
        sb.append(ste.methodName)
        sb.append("] ")
        sb.append(message)
        return sb.toString()
    }
}