/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
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
                "-expression=@file:CompilerOptions(\"-Xunknown1\")"
            ),
            expectedExitCode = 1,
            expectedSomeErrPatterns = listOf(
                "unresolved reference\\W*CompilerOptions"
            ),
        )
        runWithK2JVMCompiler(
            arrayOf(
                "-cp", getMainKtsClassPath().joinToString(File.pathSeparator),
                "-Xdefault-script-extension=.main.kts",
                "-expression=@file:CompilerOptions(\"-Xunknown1\")"
            ),
            expectedExitCode = 0,
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
            listOf("\\[1, 2\\]"),
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

    @Test
    fun testCompileScriptWithRegularKotlin() {

        fun compileVariant(vararg flags: String, withScriptInstance: Boolean = true): Pair<List<String>, ExitCode> {
            return withTempDir { tmpdir ->
                val (_, err, exitCode) = captureOutErrRet {
                    CLITool.doMainNoExit(
                        K2JVMCompiler(),
                        arrayOf(
                            "-d", tmpdir.path,
                            "-cp", getMainKtsClassPath().joinToString(File.pathSeparator),
                            *flags,
                            if (withScriptInstance)
                                "$TEST_DATA_DIR/compiler/mixedCompilation/simpleScriptInstance.kt"
                            else
                                "$TEST_DATA_DIR/compiler/mixedCompilation/nonScript.kt",
                            "$TEST_DATA_DIR/compiler/mixedCompilation/simpleScript.main.kts"
                        )
                    )
                }
                err.linesSplitTrim() to exitCode
            }
        }

        val scriptInSourceRootWarning =
            "warning: script 'simpleScript.main.kts' is not supposed to be used along with regular Kotlin sources, and will be ignored in the future versions"

        val unresolvedScriptError =
            "simpleScriptInstance.kt:3:13: error: unresolved reference: SimpleScript_main"

        compileVariant("-language-version", "1.7").let { (errLines, exitCode) ->
            Assert.assertTrue(errLines.any { it.startsWith(scriptInSourceRootWarning) })
            Assert.assertEquals(ExitCode.OK, exitCode)
        }

        compileVariant("-language-version", "1.7", "-Xallow-any-scripts-in-source-roots").let { (errLines, exitCode) ->
            Assert.assertTrue(errLines.none { it.startsWith(scriptInSourceRootWarning) })
            Assert.assertEquals(ExitCode.OK, exitCode)
        }

        compileVariant("-language-version", "1.9").let { (errLines, exitCode) ->
            if (errLines.none { it.endsWith(unresolvedScriptError) }) {
                Assert.fail("Expecting unresolved reference: SimpleScript_main error, got:\n${errLines.joinToString("\n")}")
            }
            Assert.assertEquals(ExitCode.COMPILATION_ERROR, exitCode)
        }

        compileVariant("-language-version", "1.9", withScriptInstance = false).let { (errLines, exitCode) ->
            Assert.assertTrue(errLines.none { it.startsWith(scriptInSourceRootWarning) })
            Assert.assertEquals(ExitCode.OK, exitCode)
        }

        compileVariant("-language-version", "1.9", "-Xallow-any-scripts-in-source-roots").let { (errLines, exitCode) ->
            Assert.assertTrue(errLines.none {
                it.endsWith(unresolvedScriptError) || it.startsWith(scriptInSourceRootWarning)
            })
            Assert.assertEquals(ExitCode.OK, exitCode)
        }
    }

    @Test
    fun testScriptMainKtsDiscovery() {
        withTempDir { tmpdir ->

            fun compileSuccessfullyGetStdErr(fileArg: String): List<String> {
                val (_, err, ret) = captureOutErrRet {
                    CLITool.doMainNoExit(
                        K2JVMCompiler(),
                        arrayOf(
                            "-P", "plugin:kotlin.scripting:disable-script-definitions-autoloading=true",
                            "-cp", getMainKtsClassPath().joinToString(File.pathSeparator), "-d", tmpdir.path,
                            "-Xuse-fir-lt=false",
                            "-Xallow-any-scripts-in-source-roots", "-verbose", fileArg
                        )
                    )
                }
                Assert.assertEquals(0, ret.code)
                return err.linesSplitTrim()
            }

            val loadMainKtsMessage = "logging: configure scripting: loading script definition class org.jetbrains.kotlin.mainKts.MainKtsScript using classpath"

            val res1 = compileSuccessfullyGetStdErr("$TEST_DATA_DIR/compiler/mixedCompilation/nonScript.kt")
            Assert.assertTrue(res1.none { it.startsWith(loadMainKtsMessage) })

            val res2 = compileSuccessfullyGetStdErr("$TEST_DATA_DIR/compiler/mixedCompilation/simpleScript.main.kts")
            Assert.assertTrue(res2.any { it.startsWith(loadMainKtsMessage) })
        }
    }

    private fun getMainKtsClassPath(): List<File> {
        return listOf(
            File("dist/kotlinc/lib/kotlin-main-kts.jar").also {
                Assert.assertTrue("kotlin-main-kts.jar not found, run dist task: ${it.absolutePath}", it.exists())
            }
        )
    }
}

