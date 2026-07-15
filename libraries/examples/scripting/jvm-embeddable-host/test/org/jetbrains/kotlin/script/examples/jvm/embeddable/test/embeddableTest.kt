/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.embeddable.test

import org.jetbrains.kotlin.script.examples.jvm.embeddable.host.evalFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics

class SimpleTest {

    @Test
    fun testSimple() {
        // see comments in the script file
        val res = evalFile(File("testData/useGuava.simplescript.kts"))

        Assertions.assertTrue(
            res is ResultWithDiagnostics.Success,
            "test failed:\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}"
        )
    }
}
