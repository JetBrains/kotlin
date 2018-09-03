/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createCompilationConfigurationFromTemplate
import kotlin.script.templates.standard.SimpleScriptTemplate

class ScriptingHostTest {

    @Test
    fun testSimpleUsage() {
        val greeting = "Hello from script!"
        val output = captureOut {
            evalScript("println(\"$greeting\")")
        }.trim()
        Assert.assertEquals(greeting, output)
    }
}

internal fun evalScript(script: String) {
    val compilationConfiguration = createCompilationConfigurationFromTemplate<SimpleScriptTemplate>()
    val res = BasicJvmScriptingHost().eval(script.toScriptSource(), compilationConfiguration, null)
    if (res is ResultWithDiagnostics.Failure) {
        throw Exception("Compilation/evaluation failed:\n  ${res.reports.joinToString("\n  ") { it.exception?.toString() ?: it.message }}")
    }
}

internal fun captureOut(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return outStream.toString()
}
