package com.fountain.launcher.compliance

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The policy is published from docs/privacy.md via GitHub Pages and bundled into the
 * app so About works offline. If the two ever diverge, the app shows users a different
 * policy than the one Play was given. Fail loudly instead.
 *
 * Gradle runs unit tests with the module directory (app/) as the working directory.
 */
class PrivacyPolicyMirrorTest {

    private val published = File("../docs/privacy.md")
    private val bundled = File("src/main/assets/legal/privacy-policy.md")

    @Test
    fun `both copies of the privacy policy exist`() {
        assertTrue("missing ${published.path}", published.isFile)
        assertTrue("missing ${bundled.path}", bundled.isFile)
    }

    @Test
    fun `bundled policy is identical to the published policy`() {
        assertEquals(
            "docs/privacy.md and the bundled asset have diverged — re-copy one over the other",
            published.readText().replace("\r\n", "\n"),
            bundled.readText().replace("\r\n", "\n"),
        )
    }
}
