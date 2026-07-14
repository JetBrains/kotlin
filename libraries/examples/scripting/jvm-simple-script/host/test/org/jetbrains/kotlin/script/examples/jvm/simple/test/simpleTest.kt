/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.simple.test

import org.jetbrains.kotlin.script.examples.jvm.simple.host.evalFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics

class SimpleTest {

    @Test
    fun testSimple() {
        val res = evalFile(File("testData/hello.simplescript.kts"))

        Assertions.assertTrue(
            res is ResultWithDiagnostics.Success,
            "test failed:\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}"
        )
    }

    @Test
    fun testError() {
        val res = evalFile(File("testData/error.simplescript.kts"))

        Assertions.assertTrue(
            res is ResultWithDiagnostics.Failure && res.reports.any { it.message.contains("Unresolved reference 'abracadabra'.") },
            "test failed - expecting a failure with the message \"Unresolved reference 'abracadabra'.\" but received " +
                    (if (res is ResultWithDiagnostics.Failure) "failure" else "success") +
                    ":\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}"
        )
    }
}
