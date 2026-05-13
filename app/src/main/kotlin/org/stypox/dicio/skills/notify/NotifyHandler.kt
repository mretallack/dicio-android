package org.stypox.dicio.skills.notify

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import org.stypox.dicio.di.WakeDeviceWrapper
import org.stypox.dicio.io.wake.WakeService
import javax.inject.Inject

@AndroidEntryPoint
class NotifyHandler: NotificationListenerService() {
    companion object Companion {
        private const val TAG: String = "NotifyHandler"
        private const val WATCHDOG_INTERVAL_MS = 30_000L
        var Instance: NotifyHandler? = null
    }

    @Inject
    lateinit var wakeDevice: WakeDeviceWrapper

    private var lastWatchdogCheck = 0L

    override fun onCreate() {
        super.onCreate()
        Instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        Instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        checkWakeService()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        checkWakeService()
    }

    private fun checkWakeService() {
        val now = System.currentTimeMillis()
        if (now - lastWatchdogCheck < WATCHDOG_INTERVAL_MS) return
        lastWatchdogCheck = now

        // Only restart if a wake device is configured and the service isn't running
        if (wakeDevice.state.value != null && !WakeService.isRunning()) {
            Log.w(TAG, "WakeService not running, restarting")
            WakeService.start(this)
        }
    }

    fun getActiveNotificationsList(): List<Notification> {
        // getActiveNotifications() can only be run in NotificationListenerService() class
        return getActiveNotifications().mapNotNull { notification ->
            val title = notification.notification.extras.getString("android.title")
            val message = notification.notification.extras.getString("android.text")
            if (title.isNullOrBlank() && message.isNullOrBlank()) {
                return@mapNotNull null // skip empty notifications (like from android_system)
            }
            Log.e(TAG, notification.notification.extras.toString())

            val appName = try {
                val appInfo = packageManager.getApplicationInfo(notification.packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                Log.e(TAG, "Could not get app name", e)
                ""
            }

            return@mapNotNull Notification(appName, title ?: "", message ?: "")
        }
    }
}

/**
 * Either [message] or [title] will be non-empty
 */
data class Notification(
    val appName: String,
    val title: String,
    val message: String,
)