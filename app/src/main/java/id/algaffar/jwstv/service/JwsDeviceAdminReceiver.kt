package id.algaffar.jwstv.service

import android.app.Activity
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Device Admin / Device Owner receiver for full OS-level Android TV Kiosk Mode.
 *
 * When provisioned via `adb shell dpm set-device-owner id.algaffar.jwstv/.service.JwsDeviceAdminReceiver`,
 * this enables silent Lock Task Mode (`startLockTask()`) without confirmation prompts,
 * locking the TV/STB strictly to JWS TV Masjid Al-Gaffar 24/7.
 */
class JwsDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        configureKioskPolicies(context)
    }

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context, JwsDeviceAdminReceiver::class.java)

        fun isDeviceOwner(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            return runCatching { dpm.isDeviceOwnerApp(context.packageName) }.getOrDefault(false)
        }

        fun configureKioskPolicies(context: Context) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return
            if (!runCatching { dpm.isDeviceOwnerApp(context.packageName) }.getOrDefault(false)) return

            val admin = componentName(context)
            runCatching {
                dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
                }
            }
        }

        fun startKioskLockTaskIfDeviceOwner(activity: Activity) {
            if (!isDeviceOwner(activity)) return
            configureKioskPolicies(activity)
            runCatching {
                activity.startLockTask()
            }
        }

        fun stopKioskLockTask(activity: Activity): Boolean {
            return runCatching {
                activity.stopLockTask()
                true
            }.getOrDefault(false)
        }

        fun rebootDeviceIfOwner(context: Context): Boolean {
            if (!isDeviceOwner(context)) return false
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            return runCatching {
                dpm.reboot(componentName(context))
                true
            }.getOrDefault(false)
        }
    }
}
