package com.fountain.launcher.home

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable

/**
 * One launchable app on the home surface. [icon] is the app's real (colored) drawable;
 * the home surface renders it monochrome at draw time (spec §2.2), so we keep the
 * original here rather than pre-desaturating.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val component: ComponentName,
) {
    /** Grouping key for the A–Z index: first letter, or "#" for non-alphabetic. */
    val sectionKey: String
        get() {
            val c = label.firstOrNull()?.uppercaseChar() ?: '#'
            return if (c in 'A'..'Z') c.toString() else "#"
        }
}

/** A fixed quick-launch shortcut pinned on the home surface (e.g. Phone, Camera). */
data class QuickApp(
    val label: String,
    val icon: Drawable,
    val intent: Intent,
)
