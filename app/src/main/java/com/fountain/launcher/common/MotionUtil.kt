package com.fountain.launcher.common

import android.content.Context
import android.provider.Settings

/** Respects the system "remove animations" setting (quality floor). */
object MotionUtil {
    fun isReducedMotion(context: Context): Boolean = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    } catch (e: Settings.SettingNotFoundException) {
        false
    }
}
