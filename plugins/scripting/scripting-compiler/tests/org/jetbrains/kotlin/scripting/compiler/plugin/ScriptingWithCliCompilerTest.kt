/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.scripting.compiler.test.linesSplitTrim
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ScriptingWithCliCompilerTest {

    companion object {
        const val TEST_DATA_DIR = "plugins/scripting/scripting-compiler/testData"
    }

    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun testResultValue() {
        runWithK2JVMCompiler("$TEST_DATA_DIR/integration/intResult.kts", listOf("10"))
    }

    @Test
    fun testResultValueViaKotlinc() {
        runWithKotlinc("$TEST_DATA_DIR/integration/intResult.kts", listOf("10"))
    }

    @Test
    fun testStandardScriptWithDeps() {
        runWithK2JVMCompiler(
            "$TEST_DATA_DIR/integration/withDependencyOnCompileClassPath.kts", listOf("Hello from standard kts!"),
            classpath = getMainKtsClassPath()
        )
    }

    @Test
    fun testStandardScriptWithDepsViaKotlinc() {
        runWithKotlinc(
            "$TEST_DATA_DIR/integration/withDependencyOnCompileClassPath.kts", listOf("Hello from standard kts!"),
            classpath = getMainKtsClassPath()
        )
    }

    @Test
    fun testExpression() {
        runWithK2JVMCompiler(
            arrayOf(
                "-expression",
                "val x = 7; println(x * 6); for (arg in args) println(arg)",
                "--",
                "hi",
                "there"
            ),
            listOf("42", "hi", "there")
        )
    }

    @Test
    fun testExpressionAsMainKts() {
        // testing that without specifying default to .main.kts, the annotation is unresolved
        runWithK2JVMCompiler(
            arrayOf(
                "-cp", getMainKtsClassPath().joinToString(File.pathSeparator),
                "-expression",
                "\\@file:CompilerOptions(\"-Xunknown1\")"
            ),
            listOf(""),
            expectedExitCode = 1,
            expectedSomeErrPatterns = listOf(
                "unresolved reference: CompilerOptions"
            )
        )
        // it seems not possible to make a one-liner with the annotation, and
        // annotation is the easiest available distinguishing factor for the .main.kts script
        // so, considering "expecting an element" error as a success here
        runWithK2JVMCompiler(
            arrayOf(
                "-cp", getMainKtsClassPath().joinToString(File.pathSeparator),
                "-Xdefault-script-extension=.main.kts",
                "-expression",
                "\\@file:CompilerOptions(\"-Xunknown1\")"
            ),
            listOf(""),
            expectedExitCode = 1,
            expectedSomeErrPatterns = listOf(
                "expecting an element"
            )
        )
    }

    @Test
    fun testScriptAsMainKts() {
        val scriptFile = Files.createTempFile("someScript", "").toFile()
        scriptFile.writeText("@file:CompilerOptions(\"-abracadabra\")\n42")

        // testing that without specifying default to .main.kts the script with extension .txt is not recognized
        runWithK2JVMCompiler(
            arrayOf(
                "-cp", getMainKtsClassPath().joinToString(File.pathSeparator),
                "-script",
                scriptFile.path
            ),
            listOf(""),
            expectedExitCode = 1,
            expectedSomeErrPatterns = listOf(
                "unrecognized script type: someScript.+"
            )
        )
        runWithK2JVMCompiler(
            arrayOf(
                "-cp", getMainKtsClassPath().joinToString(File.pathSeparator),
                "-Xdefault-script-extension=.main.kts",
                "-script",
                scriptFile.path
            ),
            listOf(""),
            expectedExitCode = 1,
            expectedSomeErrPatterns = listOf(
                "error: invalid argument: -abracadabra"
            )
        )
        runWithK2JVMCompiler(
            arrayOf(
                "-cp", getMainKtsClassPath().joinToString(File.pathSeparator),
                "-Xdefault-script-extension=main.kts",
                "-script",
                scriptFile.path
            ),
            listOf(""),
            expectedExitCode = 1,
            expectedSomeErrPatterns = listOf(
                "error: invalid argument: -abracadabra"
            )
        )
    }

    @Test
    fun testExpressionWithComma() {
        runWithK2JVMCompiler(
            arrayOf(
                "-expression",
                "listOf(1,2)"
            ),
            listOf("\\[1, 2\\]")
        )
    }

    @Test
    fun testJdkModules() {
        // actually tests anything on JDKs 9+, on pre-9 it always works because JDK is not modularized anyway
        runWithKotlinc(
            arrayOf(
                "-Xadd-modules=java.sql",
                "-expression",
                "println(javax.sql.DataSource::class.java)"
            ),
            listOf("interface javax.sql.DataSource")
        )
    }

    @Test
    fun testExceptionWithCause() {
        val (_, err, _) = captureOutErrRet {
            CLITool.doMainNoExit(
                K2JVMCompiler(),
                arrayOf(
                    "-script",
                    "$TEST_DATA_DIR/integration/exceptionWithCause.kts"
                )
            )
        }
        val filteredErr = err.linesSplitTrim().filterNot { it.startsWith("WARN: ") }
        Assert.assertEquals(
            """
                java.lang.Exception: Top
	                    at ExceptionWithCause.<init>(exceptionWithCause.kts:8)
                Caused by: java.lang.Exception: Oh no
	                    at ExceptionWithCause.<init>(exceptionWithCause.kts:5)
                Caused by: java.lang.Exception: Error!
	                    at ExceptionWithCause.<init>(exceptionWithCause.kts:3)
            """.trimIndent().linesSplitTrim(),
            filteredErr
        )
    }

    private fun getMainKtsClassPath(): List<File> {
        return listOf(
            File("dist/kotlinc/lib/kotlin-main-kts.jar").also {
                Assert.assertTrue("kotlin-main-kts.jar not found, run dist task: ${it.absolutePath}", it.exists())
            }
        )
    }
}

