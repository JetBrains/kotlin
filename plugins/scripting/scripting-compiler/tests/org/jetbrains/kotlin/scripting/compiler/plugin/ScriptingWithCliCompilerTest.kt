/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import java.io.File
import junit.framework.Assert.*
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ScriptingWithCliCompilerTest {

    companion object {
        const val TEST_DATA_DIR = "plugins/scripting/scripting-compiler/testData"
    }

    @Test
    fun testResultValue() {
        runWithK2JVMCompiler("$TEST_DATA_DIR/integration/intResult.kts", listOf("10"))
    }
}

fun runWithK2JVMCompiler(
    scriptPath: String,
    expectedOutPatterns: List<String> = emptyList(),
    expectedExitCode: Int = 0
) {
    val mainKtsJar = File("dist/kotlinc/lib/kotlin-main-kts.jar")
    assertTrue("kotlin-main-kts.jar not found, run dist task: ${mainKtsJar.absolutePath}", mainKtsJar.exists())

    val (out, err, ret) = captureOutErrRet {
        CLITool.doMainNoExit(
            K2JVMCompiler(),
            arrayOf("-kotlin-home", "dist/kotlinc", "-cp", mainKtsJar.absolutePath, "-script", scriptPath)
        )
    }
    try {
        val outLines = out.lines()
        assertEquals(expectedOutPatterns.size, outLines.size)
        for ((expectedPattern, actualLine) in expectedOutPatterns.zip(outLines)) {
            assertTrue(
                "line \"$actualLine\" do not match with expected pattern \"$expectedPattern\"",
                Regex(expectedPattern).matches(actualLine)
            )
        }
        assertEquals(expectedExitCode, ret.code)
    } catch (e: Throwable) {
        println("OUT:\n$out")
        println("ERR:\n$err")
        throw e
    }
}


internal fun <T> captureOutErrRet(body: () -> T): Triple<String, String, T> {
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()
    val prevOut = System.out
    val prevErr = System.err
    System.setOut(PrintStream(outStream))
    System.setErr(PrintStream(errStream))
    val ret = try {
        body()
    } finally {
        System.out.flush()
        System.err.flush()
        System.setOut(prevOut)
        System.setErr(prevErr)
    }
    return Triple(outStream.toString().trim(), errStream.toString().trim(), ret)
}
