/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.cli

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.junit.jupiter.api.Test

class ScriptingCliSmokeTest : KotlinIntegrationTestBase() {
    private fun testData(name: String) =
        ForTestCompileRuntime.transformTestDataPath("plugins/scripting/scripting-tests/testData/cli/smoke/$name")

    private fun runCompiler(name: String, vararg arguments: String) {
        val args = arrayListOf("-cp", getCompilerLib().resolve("kotlin-compiler.jar").path, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
        args.addAll(arguments)
        runJava(testData(name), "script", *args.toTypedArray())
    }

    @Test
    fun testSimpleScript() {
        runCompiler("simpleScript", "-script", "script.kts", "hi", "there")
    }

    @Test
    fun testScriptDashedArgs() {
        runCompiler("scriptDashedArgs", "-script", "script.kts", "--", "hi", "-name", "Marty", "--", "there")
    }

    @Test
    fun testScriptException() {
        runCompiler("scriptException", "-script", "script.kts")
    }

    @Test
    fun testScriptFlushBeforeShutdown() {
        runCompiler("scriptFlushBeforeShutdown", "-script", "script.kts")
    }

    @Test
    fun testCompileScript() {
        runCompiler("compileScript", "-Xallow-any-scripts-in-source-roots", "script.kts", "-d", tmpdir.resolve("script.jar").path)
    }
}
