/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import junit.framework.Assert
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.junit.Test
import java.io.File

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
    fun testExpressionWithComma() {
        runWithK2JVMCompiler(
            arrayOf(
                "-expression",
                "listOf(1,2)"
            ),
            listOf("\\[1, 2\\]")
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

