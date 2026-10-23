package com.fountain.launcher.common

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

/** Turns the screen off via device-admin force-lock (the "double-tap to lock" feature). */
object DeviceLock {

    private fun component(context: Context) =
        ComponentName(context, FountainDeviceAdminReceiver::class.java)

    private fun dpm(context: Context) =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isAdminActive(context: Context): Boolean =
        dpm(context).isAdminActive(component(context))

    /**
     * Opens the system "Activate device admin app?" prompt. Must be started from an Activity
     * without FLAG_ACTIVITY_NEW_TASK: Settings' DeviceAdminAdd finishes itself immediately
     * ("Cannot start ADD_DEVICE_ADMIN as a new task") if launched as a new task.
     */
    fun requestAdmin(context: Context) {
        val activity = findActivity(context) ?: return
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Lets Fountain turn the screen off when you double-tap the fountain.",
            )
        activity.startActivity(intent)
    }

    private fun findActivity(context: Context): Activity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Gives the admin right back. Android refuses to uninstall an app while it is an
     * active device administrator, so without an in-app way to revoke this, enabling
     * double-tap-to-lock would leave the user hunting through
     * Settings → Device admin apps before they could remove Fountain. An app that
     * resists uninstallation is also a pattern Play treats as a malware signal.
     */
    fun removeAdmin(context: Context) {
        if (isAdminActive(context)) {
            dpm(context).removeActiveAdmin(component(context))
        }
    }

    /** Lock the screen if admin is granted; otherwise open the grant prompt. */
    fun lockNow(context: Context) {
        if (isAdminActive(context)) {
            dpm(context).lockNow()
        } else {
            requestAdmin(context)
        }
    }
}
