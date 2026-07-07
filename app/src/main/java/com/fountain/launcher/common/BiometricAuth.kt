package com.fountain.launcher.common

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Unlocks the lock overlay with the device's own credential (spec §2.4). We never store
 * or handle a PIN — the system prompt does, reusing the user's real PIN/pattern/biometric.
 * On the Note 9 (API 29) androidx.biometric routes device-credential auth through the
 * keyguard confirm flow; on R+ it uses the unified authenticator API.
 */
object BiometricAuth {

    fun findActivity(context: Context): FragmentActivity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /** True if a biometric or device credential is available to prompt for. */
    fun canAuthenticate(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            return BiometricManager.from(context).canAuthenticate(authenticators) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }
        // < R: rely on the keyguard for device-credential availability.
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguard.isDeviceSecure
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                    onSuccess()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    onFailure()
                // onAuthenticationFailed (a single wrong attempt) keeps the prompt open.
            },
        )

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Fountain")
            .setSubtitle("Confirm it's you to leave the lock screen")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }

        prompt.authenticate(builder.build())
    }
}
