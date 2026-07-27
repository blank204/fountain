package com.fountain.launcher.compliance

/**
 * Fountain's Google Play policy surface, kept in one package so it is findable.
 *
 * Fountain is not an accessibility tool, so it cannot set isAccessibilityTool=true.
 * Play therefore requires an in-app disclosure that names the data accessed, explains
 * how it is used and shared, and takes affirmative consent — shown during normal app
 * usage, not buried in a menu. DisclosureCopyTest pins the required content.
 */
enum class SensitiveService { ACCESSIBILITY, NOTIFICATION_LISTENER }

/** The three sections Play's policy demands, plus the two button labels. */
data class DisclosureCopy(
    val title: String,
    val whatIsAccessed: String,
    val howItIsUsed: String,
    val howItIsShared: String,
    val consentLabel: String,
    val dismissLabel: String,
)

fun disclosureCopyFor(service: SensitiveService): DisclosureCopy = when (service) {
    SensitiveService.ACCESSIBILITY -> DisclosureCopy(
        title = "Before you enable force-kick",
        whatIsAccessed =
            "Fountain reads only the package name of the app currently in front. " +
                "It cannot read screen content, text you type, or anything inside your apps.",
        howItIsUsed =
            "Noticing when a time-gated app opens, so Fountain can ask how long you want — " +
                "and sending you back home when that time is up.",
        howItIsShared =
            "Nowhere. It is held in memory, never saved, never transmitted, never shared " +
                "with anyone. Fountain has no internet permission and is incapable of " +
                "sending anything off your device.",
        consentLabel = "I understand — continue",
        dismissLabel = "Not now",
    )

    SensitiveService.NOTIFICATION_LISTENER -> DisclosureCopy(
        title = "Before you enable the inbox",
        whatIsAccessed =
            "For apps you choose, Fountain reads the app name, title, text and time of " +
                "notifications they post.",
        howItIsUsed =
            "Showing those notifications in Fountain's own inbox, and muting the apps you " +
                "have asked to mute.",
        howItIsShared =
            "Never transmitted, never shared. It is saved only in Fountain's database on " +
                "this device, and you can erase all of it any time from Settings → Inbox.",
        consentLabel = "I understand — continue",
        dismissLabel = "Not now",
    )
}
