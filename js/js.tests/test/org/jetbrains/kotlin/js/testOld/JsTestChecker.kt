/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.js.engine.ScriptEngine
import org.jetbrains.kotlin.js.engine.ScriptEngineV8
import org.jetbrains.kotlin.js.engine.loadFiles
import org.jetbrains.kotlin.js.test.utils.KOTLIN_TEST_INTERNAL
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import org.junit.Assert
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

internal const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
private const val ESM_EXTENSION = ".mjs"

fun createScriptEngine(): ScriptEngine = ScriptEngineV8()

private fun String.escapePath(): String {
    return replace("\\", "/")
}

fun ScriptEngine.runTestFunction(
    testModuleName: String?,
    testPackageName: String?,
    testFunctionName: String,
    withModuleSystem: Boolean,
    testFunctionArgs: String = "",
    entryModulePath: String? = null,
): String {
    if (withModuleSystem && testModuleName == null && entryModulePath == null) {
        error("Entry point was not found. Please specify ENTRY_ES_MODULE directive near js file, if this is ES Modules test.")
    }
    var script = when {
        entryModulePath != null && entryModulePath.endsWith(ESM_EXTENSION) -> "globalThis".also {
            eval("import('${entryModulePath.escapePath()}').then(module => Object.assign(globalThis, module)).catch(console.error)")
        }
        withModuleSystem -> "$KOTLIN_TEST_INTERNAL.require('" + testModuleName!! + "')"
        testModuleName === null -> "this"
        else -> testModuleName
    }

    if (testPackageName !== null) {
        script += ".$testPackageName"
    }

    script += ".$testFunctionName($testFunctionArgs)"

    return eval(script)
}

object V8JsTestChecker {
    fun check(
        files: List<String>,
        testModuleName: String?,
        testPackageName: String?,
        testFunctionName: String,
        expectedResult: String,
        withModuleSystem: Boolean,
        entryModulePath: String? = null,
    ) {
        val actualResult = run(files, testModuleName, testPackageName, testFunctionName, "", withModuleSystem, entryModulePath)
        Assert.assertEquals(expectedResult, actualResult.normalize())
    }

    fun checkWithTestFunctionArgs(
        files: List<String>,
        testModuleName: String?,
        testPackageName: String?,
        testFunctionName: String,
        testFunctionArgs: String,
        expectedResult: String,
        withModuleSystem: Boolean,
        entryModulePath: String? = null
    ) {
        val actualResult = run(files, testModuleName, testPackageName, testFunctionName, testFunctionArgs, withModuleSystem, entryModulePath)
        Assert.assertEquals(expectedResult, actualResult.normalize())
    }

    private fun run(
        files: List<String>,
        testModuleName: String?,
        testPackageName: String?,
        testFunctionName: String,
        testFunctionArgs: String,
        withModuleSystem: Boolean,
        entryModulePath: String? = null,
    ) = run(files) {
        runTestFunction(testModuleName, testPackageName, testFunctionName, withModuleSystem, testFunctionArgs, entryModulePath)
    }


    fun run(files: List<String>) {
        run(files) { "" }
    }

    fun checkStdout(files: List<String>, expectedResult: String) {
        val newFiles = files
            .mapIndexed { index, s ->
                if (index == files.size - 1) {
                    val file = File(s)
                    val lines = file.readText().lines().toMutableList()
                    lines.add(lines.size - 6, JS_IR_OUTPUT_REWRITE)
                    val newFile = file.withSuffixAndExtension("_modified", "js")
                    newFile.writeText(lines.joinToString("\n"))
                    newFile.absolutePath
                } else {
                    s
                }
            }
        run(newFiles) {
            val actualResult = eval(GET_KOTLIN_OUTPUT)
            Assert.assertEquals(expectedResult, actualResult.normalize())
            ""
        }
    }

    private fun String.normalize() = StringUtil.convertLineSeparators(this)

    fun run(files: List<String>, f: ScriptEngine.() -> String): String {
        periodicScriptEngineRecreate { engineTL.remove() }

        val engine = engineTL.get()
        return try {
            engine.loadFiles(files)
            engine.f()
        } finally {
            engine.reset()
        }
    }

    private var engineUsageCnt = AtomicInteger(0)

    private val engineTL = object : ThreadLocal<ScriptEngineV8>() {
        override fun initialValue() = ScriptEngineV8()
        override fun remove() {
            get().release()
            super.remove()
        }
    }

    private fun periodicScriptEngineRecreate(doCleanup: () -> Unit) {
        if (engineUsageCnt.getAndIncrement() > SCRIPT_ENGINE_REUSAGE_LIMIT) {
            engineUsageCnt.set(0)
            doCleanup()
        }
    }

    private const val SCRIPT_ENGINE_REUSAGE_LIMIT = 100
}

const val GET_KOTLIN_OUTPUT = "main.get_output().buffer_1"

private val JS_IR_OUTPUT_REWRITE = """
    set_output(new BufferedOutput())
    _.get_output = get_output
""".trimIndent()
