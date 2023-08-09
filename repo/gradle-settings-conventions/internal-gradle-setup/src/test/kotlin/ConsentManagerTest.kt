/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.apache.tools.ant.filters.StringInputStream
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class ConsentConfiguration(
    val globalConsent: Boolean?,
    val localConsent: Boolean?,
    val expected: Boolean?,
)

class ConsentManagerTest {
    @TempDir
    lateinit var workingDir: Path

    private val localPropertiesFile by lazy {
        workingDir.resolve("local.properties")
    }

    private val modifier by lazy {
        LocalPropertiesModifier(localPropertiesFile.toFile())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getConfigurationsForParameterizedConsentCheckingTest")
    fun testConsentChecker(consentConfiguration: ConsentConfiguration) {
        when (consentConfiguration.localConsent) {
            true -> localPropertiesFile.toFile().writeText(USER_CONSENT_MARKER)
            false -> localPropertiesFile.toFile().writeText(USER_REFUSAL_MARKER)
            null -> {
                // do nothing
            }
        }
        assertEquals(consentConfiguration.expected, ConsentManager(modifier, consentConfiguration.globalConsent).getUserDecision())
    }

    @Test
    fun testConsentRequestAgree() = testUserDecision("yes", true, USER_CONSENT_MARKER)

    @Test
    fun testConsentRequestAgreeWithDetailsLink() = testUserDecision(
        "yes",
        true,
        USER_CONSENT_MARKER_WITH_DETAILS_LINK.formatWithLink("https://example.org"),
        consentDetailsLink = "https://example.org"
    )

    @Test
    fun testConsentRequestRefusal() = testUserDecision("no", false, USER_REFUSAL_MARKER)

    @Test
    fun testConsentRequestAnswerAfterBadInput() = testUserDecision("\nasdasd\nno", false, USER_REFUSAL_MARKER, 3)

    private fun testUserDecision(
        prompt: String,
        expectedDecision: Boolean,
        expectedLine: String,
        promptCount: Int = 1,
        consentDetailsLink: String? = null,
    ) {
        StringInputStream(prompt).bufferedReader().use { input ->
            val outputStream = ByteArrayOutputStream(255)
            PrintStream(outputStream).use { printStream ->
                val consentManager = ConsentManager(modifier, null, input, printStream)
                val userDecision = consentManager.askForConsent(consentDetailsLink)
                assertEquals(expectedDecision, userDecision)
                val content = Files.readAllLines(localPropertiesFile)
                assertTrue(content.contains(expectedLine))
                val output = String(outputStream.toByteArray())
                assertContainsExactTimes(output, USER_CONSENT_REQUEST, 1)
                assertContainsExactTimes(output, PROMPT_REQUEST, promptCount)

                if (consentDetailsLink != null) {
                    assertContainsExactTimes(output, USER_CONSENT_DETAILS_LINK_TEMPLATE.formatWithLink(consentDetailsLink), 1)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        private fun getConfigurationsForParameterizedConsentCheckingTest() = Stream.of(
            named("no decision is provided", ConsentConfiguration(null, null, null)),
            named("local consent given with no global decision", ConsentConfiguration(null, true, true)),
            named("local consent refused with no global decision", ConsentConfiguration(null, false, false)),
            named("global consent refused with no local decision", ConsentConfiguration(false, null, false)),
            named("local given consent takes priority over the global refused one", ConsentConfiguration(false, true, true)),
            named("both local and global consents are refused", ConsentConfiguration(false, false, false)),
            named("global consent refused with no local decision", ConsentConfiguration(true, null, true)),
            named("both local and global consents are given", ConsentConfiguration(true, true, true)),
            named("local refused consent takes priority over the global given one", ConsentConfiguration(true, false, false)),
        )
    }
}