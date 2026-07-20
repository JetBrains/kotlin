/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.cli

import org.jetbrains.kotlin.cli.CliProcessUtils
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.junit.jupiter.api.Test

class ScriptingLauncherTest : TestCaseWithTmpdir() {
    private val testDataDirectory get() = ForTestCompileRuntime.transformTestDataPath("plugins/scripting/scripting-tests/testData/cli/launcher").absolutePath

    private fun runProcess(
        executableName: String,
        vararg args: String,
        expectedStdout: String = "",
        expectedStderr: String = "",
        expectedExitCode: Int = 0,
    ) {
        CliProcessUtils.runProcess(
            executableName,
            *args,
            expectedStdout = expectedStdout,
            expectedStderr = expectedStderr,
            expectedExitCode = expectedExitCode,
            testDataDirectory = testDataDirectory,
            tmpdir = tmpdir,
        )
    }

    @Test
    fun testKotlincJvmScriptWithClassPathFromSysProp() {
        runProcess(
            "kotlinc-jvm",
            "-script",
            "$testDataDirectory/classPathPropTest.kts",
            expectedStdout = "kotlin-compiler.jar\n"
        )
    }

    @Test
    fun testRunnerExpression() {
        runProcess(
            "kotlinr",
            "-e",
            "val x = 2; (args + listOf(2,1).map { (it * x).toString() }).joinToString()",
            "--",
            "a",
            "b",
            expectedStdout = "a, b, 4, 2\n"
        )
    }

    @Test
    fun testRunnerExpressionK2() {
        runProcess(
            "kotlinr",
            CommonCompilerArguments::languageVersion.cliArgument, LanguageVersion.FIRST_NON_DEPRECATED.versionString, "-e",
            "println(args.joinToString())",
            "-a",
            "b",
            expectedStdout = "-a, b\n",
        )
    }

    @Test
    fun testCommandlineProcessing() {
        runProcess(
            "kotlinr",
            "-e",
            "println(args.joinToString())",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
        runProcess(
            "kotlinr",
            "-e",
            "println(args.joinToString())",
            "--",
            "-e",
            "b",
            expectedStdout = "-e, b\n"
        )
        runProcess(
            "kotlinr",
            "$testDataDirectory/printargs.kts",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
        runProcess(
            "kotlinr",
            "$testDataDirectory/printargs.kts",
            "--",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
    }

    @Test
    fun testScriptWithXArguments() {
        runProcess(
            "kotlinr", K2JVMCompilerArguments::noInline.cliArgument, "$testDataDirectory/noInline.kts",
            expectedExitCode = 3,
            expectedStderr = """java.lang.IllegalAccessError: tried to access method kotlin.io.ConsoleKt.println(Ljava/lang/Object;)V from class NoInline
	at NoInline.<init>(noInline.kts:1)
"""
        )
        runProcess("kotlinr", "$testDataDirectory/noInline.kts", expectedStdout = "OK\n")
    }

    @Test
    fun testNoStdLib() {
        runProcess("kotlinr", "-e", "println(42)", expectedStdout = "42\n")
        runProcess(
            "kotlinr", "-no-stdlib", "-e", "println(42)",
            expectedExitCode = 1,
            expectedStderr = """
                script.kts:1:1: error: unresolved reference 'println'.
                println(42)
                ^
                """.trimIndent()
        )
    }

    @Test
    fun testHowToRunExpression() {
        runProcess(
            "kotlinr", "-howtorun", "jar", "-e", "println(args.joinToString())", "-a", "b",
            expectedExitCode = 1, expectedStderr = "error: expression evaluation is not compatible with -howtorun argument jar\n"
        )
        runProcess(
            "kotlinr", "-howtorun", "script", "-e", "println(args.joinToString())", "-a", "b",
            expectedStdout = "-a, b\n"
        )
    }

    @Test
    fun testHowToRunScript() {
        runProcess(
            "kotlinr", "-howtorun", "classfile", "$testDataDirectory/printargs.kts", "--", "-a", "b",
            expectedExitCode = 1, expectedStderr = "error: could not find or load main class \$TESTDATA_DIR\$/printargs.kts\n"
        )
        runProcess(
            "kotlinr", "-howtorun", "script", "$testDataDirectory/printargs.kts", "--", "-a", "b",
            expectedStdout = "-a, b\n"
        )
    }

    @Test
    fun testHowToRunCustomScript() {
        runProcess(
            "kotlinr", "$testDataDirectory/noInline.myscript",
            expectedExitCode = 1, expectedStderr = "error: could not find or load main class \$TESTDATA_DIR\$/noInline.myscript\n"
        )
        runProcess(
            "kotlinr", "-howtorun", "script", "$testDataDirectory/noInline.myscript",
            expectedExitCode = 1,
            expectedStderr = "error: unrecognized script type: noInline.myscript; Specify path to the script file as the first argument\n"
        )
        runProcess(
            "kotlinr",
            K2JVMCompilerArguments::allowAnyScriptsInSourceRoots.cliArgument,
            "-howtorun",
            ".kts",
            "$testDataDirectory/noInline.myscript",
            expectedExitCode = 1,
            expectedStderr = """plugins/scripting/scripting-tests/testData/cli/launcher/noInline.myscript:1:7: error: unresolved reference 'CompilerOptions'.
@file:CompilerOptions("-Xno-inline")
      ^
"""
        )
        runProcess(
            "kotlinr", "-howtorun", ".main.kts",
            "-P", "plugin:kotlin.scripting:disable-script-compilation-cache=true",
            "$testDataDirectory/noInline.myscript",
            expectedExitCode = 3,
            expectedStderr = """java.lang.IllegalAccessError: tried to access method kotlin.io.ConsoleKt.println(Ljava/lang/Object;)V from class NoInline
	at NoInline.<init>(noInline.myscript:3)
"""
        )
    }
}
