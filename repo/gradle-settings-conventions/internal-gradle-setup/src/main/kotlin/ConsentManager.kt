/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import java.io.BufferedReader
import java.io.PrintStream

private val isInIdeaSync
    get() = System.getProperty("idea.sync.active").toBoolean()

internal const val USER_CONSENT_MARKER = "# This line indicates that you have chosen to enable automatic configuration of properties."

internal const val USER_REFUSAL_MARKER =
    "# This line indicates that you have chosen to disable automatic configuration of properties. If you want to enable it, remove this line."

internal val USER_CONSENT_REQUEST = """

    ! ATTENTION REQUIRED !
    Most probably you're a developer from the Kotlin team. We are asking for your consent for automatic configuration of local.properties file
    for providing some optimizations and collecting additional debug information.

""".trimIndent()

internal const val PROMPT_REQUEST = "Do you agree with this? Please answer with 'yes' or 'no': "

internal class ConsentManager(
    private val modifier: LocalPropertiesModifier,
    private val input: BufferedReader = System.`in`.bufferedReader(),
    private val output: PrintStream = System.out,
) {
    fun getUserDecision() = when {
        modifier.initiallyContains(USER_REFUSAL_MARKER) -> false
        modifier.initiallyContains(USER_CONSENT_MARKER) -> true
        isInIdeaSync -> false
        else -> null
    }

    fun askForConsent(): Boolean {
        output.println(USER_CONSENT_REQUEST)
        while (true) {
            output.println(PROMPT_REQUEST)
            when (input.readLine()) {
                "yes" -> {
                    output.println("You've given the consent")
                    modifier.putLine(USER_CONSENT_MARKER)
                    return true
                }
                "no" -> {
                    output.println("You've refused to give the consent")
                    modifier.putLine(USER_REFUSAL_MARKER)
                    return false
                }
            }
        }
    }
}