package com.fountain.launcher.compliance

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Google Play requires that a non-accessibility use of the AccessibilityService API
 * carry an in-app disclosure describing the data accessed, how it is used, and how it
 * is shared, with affirmative consent. These tests pin that content so a later
 * refactor cannot quietly remove it.
 */
class DisclosureCopyTest {

    @Test
    fun `every service has all disclosure sections populated`() {
        for (service in SensitiveService.entries) {
            val copy = disclosureCopyFor(service)
            assertTrue("$service title is blank", copy.title.isNotBlank())
            assertTrue("$service whatIsAccessed is blank", copy.whatIsAccessed.isNotBlank())
            assertTrue("$service howItIsUsed is blank", copy.howItIsUsed.isNotBlank())
            assertTrue("$service howItIsShared is blank", copy.howItIsShared.isNotBlank())
            assertTrue("$service consentLabel is blank", copy.consentLabel.isNotBlank())
            assertTrue("$service dismissLabel is blank", copy.dismissLabel.isNotBlank())
        }
    }

    @Test
    fun `every service states that data is never transmitted`() {
        for (service in SensitiveService.entries) {
            val shared = disclosureCopyFor(service).howItIsShared.lowercase()
            assertTrue(
                "$service must state data is never transmitted",
                shared.contains("never") && shared.contains("transmit"),
            )
        }
    }

    @Test
    fun `accessibility copy denies reading screen content`() {
        val copy = disclosureCopyFor(SensitiveService.ACCESSIBILITY)
        val accessed = copy.whatIsAccessed.lowercase()
        assertTrue(
            "must state only the foreground package name is read",
            accessed.contains("package name"),
        )
        assertTrue(
            "must explicitly deny reading screen content",
            accessed.contains("cannot read") && accessed.contains("screen content"),
        )
    }

    @Test
    fun `accessibility copy cites the no-internet-permission fact`() {
        val shared = disclosureCopyFor(SensitiveService.ACCESSIBILITY).howItIsShared.lowercase()
        assertTrue(
            "the strongest verifiable claim must be stated",
            shared.contains("no internet permission"),
        )
    }

    @Test
    fun `notification copy names the stored fields and how to erase them`() {
        val copy = disclosureCopyFor(SensitiveService.NOTIFICATION_LISTENER)
        val accessed = copy.whatIsAccessed.lowercase()
        assertTrue("must name the title field", accessed.contains("title"))
        assertTrue("must name the text field", accessed.contains("text"))

        val shared = copy.howItIsShared.lowercase()
        assertTrue(
            "must say the data is stored on this device",
            shared.contains("this device"),
        )
        assertTrue(
            "must tell the user how to delete it",
            shared.contains("inbox"),
        )
    }
}
