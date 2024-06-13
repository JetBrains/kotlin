/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import org.junit.Assert
import org.junit.Test
import kotlin.script.experimental.api.*

class ExternalDeclarationsResolveOrderTest {

    @Test
    fun testScriptWithConflictingParameters() {
        val res = evalString<ScriptWithConflictingConstructorParameter>("""conflictingVariable1 + conflictingVariable2.toString()""") {
            constructorArgs("OK", 42)
        }

        Assert.assertTrue(
            "test failed:\n  ${res.render()}",
            res is ResultWithDiagnostics.Success
        )

        Assert.assertEquals(
            "OK42",
            (res.valueOrThrow().returnValue as ResultValue.Value).value
        )
    }

    @Test
    fun testScriptWithConflictingParameters_k1() {
        val res = evalString<ScriptWithConflictingConstructorParameter>("""conflictingVariable1 + conflictingVariable2.toString()""") {
            constructorArgs("OK", 42)
        }

        Assert.assertTrue(
            "test failed:\n  ${res.render()}",
            res is ResultWithDiagnostics.Success
        )

        Assert.assertEquals(
            "OK42",
            (res.valueOrThrow().returnValue as ResultValue.Value).value
        )
    }
}