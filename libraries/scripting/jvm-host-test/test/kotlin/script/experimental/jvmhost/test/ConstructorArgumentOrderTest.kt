/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import org.junit.Test
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.providedProperties
import kotlin.test.assertTrue

class ConstructorArgumentsOrderTest {

    @Test
    fun testScriptWithProvidedProperties() {
        val res = evalString<ScriptWithProvidedProperties>("""println(providedString)""") {
            providedProperties("providedString" to "Hello Provided!")
        }

        assertTrue(
            res is ResultWithDiagnostics.Success,
            "test failed:\n  ${res.render()}"
        )
    }

    @Test
    fun testScriptWithImplicitReceiver() {
        val res = evalString<ScriptWithImplicitReceiver>("""println(receiverString)""") {
            implicitReceivers(ImplicitReceiverClass("Hello Receiver!"))
        }

        assertTrue(
            res is ResultWithDiagnostics.Success,
            "test failed:\n  ${res.render()}"
        )
    }

    @Test
    fun testScriptWithBoth() {
        val res = evalString<ScriptWithBoth>("""println(providedString + receiverString)""") {
            providedProperties("providedString" to "Hello")
            implicitReceivers(ImplicitReceiverClass(" Both!"))
        }

        assertTrue(
            res is ResultWithDiagnostics.Success,
            "test failed:\n  ${res.render()}"
        )
    }

    private fun ResultWithDiagnostics<EvaluationResult>.render() =
        reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception!!.printStackTrace()}" }
}
