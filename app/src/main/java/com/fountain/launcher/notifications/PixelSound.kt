package com.fountain.launcher.notifications

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.fountain.launcher.R

/**
 * Plays the original pixel-pop for captured notifications (spec §2.5). Honors the
 * system's silent/vibrate ringer and Do-Not-Disturb — a focus app shouldn't be the one
 * thing that still beeps.
 */
class PixelSound(context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var loaded = false
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val soundId = pool.load(context, R.raw.pixel_pop, 1).also {
        pool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
    }

    fun play() {
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val filter = notificationManager.currentInterruptionFilter
        val dndActive = filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        if (dndActive) return
        if (loaded) pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}
