# ProGuard/R8 rules for Fountain.
#
# R8 is enabled on release. Most of what Fountain needs is already covered:
#
#  - Components named in AndroidManifest.xml (MainActivity, GateActivity,
#    SessionService, FountainAccessibilityService, FountainNotificationListener,
#    SessionExpiryReceiver, FountainDeviceAdminReceiver, BootReceiver) are kept
#    automatically by R8 via the manifest.
#  - Room, Jetpack Compose, DataStore and AndroidX all ship consumer rules.
#  - Fountain uses no reflection, no Class.forName, no resource-name lookup and
#    no reflective JSON library, so nothing is resolved by name at runtime.
#
# Only the rules below are Fountain's own.

# The device-admin receiver is instantiated by the system from the component name
# recorded in res/xml/device_admin.xml. The manifest keep covers the class, but be
# explicit: losing this silently breaks double-tap-to-lock.
-keep class com.fountain.launcher.common.FountainDeviceAdminReceiver { *; }

# Room entities are referenced by generated code and by column name. Room's own
# consumer rules handle the DAOs and the generated database implementation; keeping
# the entity fields guards against a schema/field mismatch after shrinking.
-keepclassmembers class com.fountain.launcher.data.** {
    <init>(...);
    <fields>;
}

# Keep source file and line numbers in stack traces, then hide the original file
# name. Without this a crash report from a user is unreadable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
