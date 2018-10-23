/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.mainKts.test

import org.jetbrains.kotlin.mainKts.MainKtsScript
import org.junit.Assert
import org.junit.Test
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

fun evalFile(scriptFile: File): ResultWithDiagnostics<EvaluationResult> {

    val scriptDefinition = createJvmCompilationConfigurationFromTemplate<MainKtsScript>()

    val evaluationEnv = ScriptEvaluationConfiguration {
        constructorArgs(emptyArray<String>())
    }

    return BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), scriptDefinition, evaluationEnv)
}

class MainKtsTest {

    @Test
    fun testResolveJunit() {
        val res = evalFile(File("testData/hello-resolve-junit.main.kts"))

        Assert.assertTrue(
            "test failed:\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Success
        )
    }

    @Test
    fun testUnresolvedJunit() {
        val res = evalFile(File("testData/hello-unresolved-junit.main.kts"))

        Assert.assertTrue(
            "test failed - expecting a failure with the message \"Unresolved reference: junit\" but received " +
                    (if (res is ResultWithDiagnostics.Failure) "failure" else "success") +
                    ":\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Failure && res.reports.any { it.message.contains("Unresolved reference: junit") })
    }

    @Test
    fun testResolveError() {
        val res = evalFile(File("testData/hello-resolve-error.main.kts"))

        Assert.assertTrue(
            "test failed - expecting a failure with the message \"Unknown set of arguments to maven resolver: abracadabra\" but received " +
                    (if (res is ResultWithDiagnostics.Failure) "failure" else "success") +
                    ":\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
            res is ResultWithDiagnostics.Failure && res.reports.any { it.message.contains("Unknown set of arguments to maven resolver: abracadabra") })
    }
}