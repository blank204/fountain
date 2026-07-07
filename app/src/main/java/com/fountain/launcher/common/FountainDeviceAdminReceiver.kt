package com.fountain.launcher.common

import android.app.admin.DeviceAdminReceiver

/**
 * Device-admin receiver whose only power is force-lock — it lets Fountain turn the screen
 * off on a double-tap. No other admin capability is requested (see res/xml/device_admin).
 */
class FountainDeviceAdminReceiver : DeviceAdminReceiver()
