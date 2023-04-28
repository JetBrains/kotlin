/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.junit.Test
import kotlin.script.experimental.api.*
import kotlin.test.assertTrue

class CapturingTest {

    @Test
    fun testScriptWithImplicitReceiverAndSimpleCapturing() {
        // Reproducing (a bit extended) scenario from KT-53947: without the fix, in the presence of the implicit receiver
        // of the same type as the receiver in the `apply` function body, the lowering was incorrectly substituting
        // the correct receiver with the accessor to the implicit one
        val res = evalString<ScriptWithImplicitReceiver>(
            """
                class C {
                    fun foo() = receiverString + "."
                }
                
                C().foo()
            """.trimIndent()
        ) {
            implicitReceivers(ImplicitReceiverClass("Ok"))
        }

        assertTrue(
            res.safeAs<ResultWithDiagnostics.Success<EvaluationResult>>()?.value?.returnValue?.safeAs<ResultValue.Value>()?.value == "Ok.",
            "test failed:\n  ${res.render()}"
        )
    }

    @Test
    fun testScriptWithImplicitReceiverAndNoCapturing() {
        // Reproducing (a bit extended) scenario from KT-53947: without the fix, in the presence of the implicit receiver
        // of the same type as the receiver in the `C2.apply` function body, the lowering was incorrectly substituting
        // the correct receiver with the accessor to the implicit one
        // `C1.foo` tests the similar situation with extension receiver.
        val res = evalString<ScriptWithImplicitReceiver>(
            """
                import kotlin.script.experimental.jvmhost.test.ImplicitReceiverClass
                
                class C1 {
                    fun run(receiver: ImplicitReceiverClass): String = receiver.foo()
                        
                    fun ImplicitReceiverClass.foo() = "--" + receiverString
                }
                
                class C2 {
                    fun apply(receiver: ImplicitReceiverClass): String =
                        "++" + receiver.receiverString
                }
                
                C2().apply(
                    ImplicitReceiverClass(
                        C1().run(ImplicitReceiverClass("Ok"))
                    )
                )
            """.trimIndent()
        ) {
            implicitReceivers(ImplicitReceiverClass("Not Ok."))
        }

        assertTrue(
            res.safeAs<ResultWithDiagnostics.Success<EvaluationResult>>()?.value?.returnValue?.safeAs<ResultValue.Value>()?.value == "++--Ok",
            "test failed:\n  ${res.render()}"
        )
    }
}