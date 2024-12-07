/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import java.io.BufferedReader
import java.io.PrintStream

private val isInIdeaSync
    get() = System.getProperty("idea.sync.active").toBoolean()

private const val linkPlaceholder = "%LINK%"

internal const val USER_CONSENT_MARKER = "# This line indicates that you have chosen to enable automatic configuration of properties."

internal const val USER_CONSENT_MARKER_WITH_DETAILS_LINK = "$USER_CONSENT_MARKER Details: $linkPlaceholder"

internal const val USER_REFUSAL_MARKER =
    "# This line indicates that you have chosen to disable automatic configuration of properties. If you want to enable it, remove this line."

internal val USER_CONSENT_REQUEST = """

    ! ATTENTION REQUIRED !
    Most probably you're a developer from the Kotlin team. We are asking for your consent for automatic configuration of local.properties file
    for providing some optimizations and collecting additional debug information.
""".trimIndent()

internal const val USER_CONSENT_DETAILS_LINK_TEMPLATE = "You can read more details here: $linkPlaceholder"

internal const val PROMPT_REQUEST = "Do you agree with this? Please answer with 'yes' or 'no': "

private const val MAX_REQUEST_ATTEMPTS = 5

internal fun String.formatWithLink(link: String) = replace(linkPlaceholder, link)

internal class ConsentManager(
    private val modifier: LocalPropertiesModifier,
    private val globalConsentGiven: Boolean? = null,
    private val input: BufferedReader = System.`in`.bufferedReader(),
    private val output: PrintStream = System.out,
) {
    fun getUserDecision(): Boolean? {
        return when {
            modifier.initiallyContains(USER_REFUSAL_MARKER) -> false
            modifier.initiallyContains(USER_CONSENT_MARKER) -> true
            globalConsentGiven == false -> false
            globalConsentGiven == true -> true
            isInIdeaSync -> false
            else -> null
        }
    }

    private fun printConsentRequest(consentDetailsLink: String? = null) {
        output.println()
        output.println(USER_CONSENT_REQUEST)
        if (consentDetailsLink != null) {
            output.println(USER_CONSENT_DETAILS_LINK_TEMPLATE.formatWithLink(consentDetailsLink))
        }
        output.println()
    }

    fun applyConsentDecision(consentGiven: Boolean, consentDetailsLink: String? = null): Boolean {
        if (consentGiven) {
            output.println("You've given the consent for the automatic configuration of local.properties")
            modifier.putLine(
                if (consentDetailsLink != null) {
                    USER_CONSENT_MARKER_WITH_DETAILS_LINK.formatWithLink(consentDetailsLink)
                } else {
                    USER_CONSENT_MARKER
                }
            )
        } else {
            output.println("You've refused to give the consent for the automatic configuration of local.properties")
            modifier.putLine(USER_REFUSAL_MARKER)
        }
        return consentGiven
    }

    fun askForConsent(consentDetailsLink: String? = null): Boolean {
        printConsentRequest(consentDetailsLink)
        repeat(MAX_REQUEST_ATTEMPTS) {
            output.println(PROMPT_REQUEST)
            return when (input.readLine()) {
                "yes" -> applyConsentDecision(true, consentDetailsLink)
                "no" -> applyConsentDecision(false, consentDetailsLink)
                else -> return@repeat
            }
        }
        // we didn't receive an answer, let's ask next time
        return false
    }
}