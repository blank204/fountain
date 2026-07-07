package com.fountain.launcher.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.fountain.launcher.accessibility.FountainAccessibilityService

/** Helpers for checking and opening the accessibility grant that powers force-kick. */
object AccessibilityUtil {

    fun isServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, FountainAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it) == expected
        }
    }

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
