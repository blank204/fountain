package com.fountain.launcher.common

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Turns the screen off via device-admin force-lock (the "double-tap to lock" feature). */
object DeviceLock {

    private fun component(context: Context) =
        ComponentName(context, FountainDeviceAdminReceiver::class.java)

    private fun dpm(context: Context) =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isAdminActive(context: Context): Boolean =
        dpm(context).isAdminActive(component(context))

    fun requestAdmin(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Lets Fountain turn the screen off when you double-tap the fountain.",
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
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
