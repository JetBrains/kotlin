/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.js.engine.ScriptEngine
import org.jetbrains.kotlin.js.engine.ScriptEngineNashorn
import org.jetbrains.kotlin.js.engine.ScriptEngineV8
import org.jetbrains.kotlin.js.engine.loadFiles
import org.junit.Assert

internal const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
private const val DIST_DIR_JS_PATH = "dist/js/"
private const val ESM_EXTENSION = ".mjs"

fun createScriptEngine(): ScriptEngine {
    return if (java.lang.Boolean.getBoolean("kotlin.js.useNashorn")) ScriptEngineNashorn() else ScriptEngineV8()
}

fun ScriptEngine.overrideAsserter() {
    eval("this['kotlin-test'].kotlin.test.overrideAsserter_wbnzx$(this['kotlin-test'].kotlin.test.DefaultAsserter);")
}

private fun String.escapePath(): String {
    return replace("\\", "/")
}

@Suppress("UNUSED_PARAMETER")
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
        withModuleSystem -> "\$kotlin_test_internal\$.require('" + testModuleName!! + "')"
        testModuleName === null -> "this"
        else -> testModuleName
    }

    if (testPackageName !== null) {
        script += ".$testPackageName"
    }

    script += ".$testFunctionName($testFunctionArgs)"

    return eval(script)
}

abstract class AbstractJsTestChecker {
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
        run(files) {
            val actualResult = eval(GET_KOTLIN_OUTPUT)
            Assert.assertEquals(expectedResult, actualResult.normalize())
            ""
        }
    }

    private fun String.normalize() = StringUtil.convertLineSeparators(this)

    protected abstract fun run(files: List<String>, f: ScriptEngine.() -> String): String
}

fun ScriptEngine.runAndRestoreContext(f: ScriptEngine.() -> String): String {
    return try {
        saveGlobalState()
        f()
    } finally {
        restoreGlobalState()
    }
}

abstract class AbstractNashornJsTestChecker : AbstractJsTestChecker() {

    private var engineUsageCnt = 0

    private var engineCache: ScriptEngineNashorn? = null

    protected val engine: ScriptEngineNashorn
        get() = engineCache ?: createScriptEngineForTest().also {
            engineCache = it
        }

    protected open fun beforeRun() {}

    override fun run(files: List<String>, f: ScriptEngine.() -> String): String {
        // Recreate the engine once in a while
        if (engineUsageCnt++ > 100) {
            engineUsageCnt = 0
            engineCache = null
        }

        beforeRun()

        return engine.runAndRestoreContext {
            loadFiles(files)
            f()
        }
    }

    protected abstract val preloadedScripts: List<String>

    protected open fun createScriptEngineForTest(): ScriptEngineNashorn {
        val engine = ScriptEngineNashorn()

        engine.loadFiles(preloadedScripts)

        return engine
    }
}

const val SETUP_KOTLIN_OUTPUT = "kotlin.kotlin.io.output = new kotlin.kotlin.io.BufferedOutput();"
const val GET_KOTLIN_OUTPUT = "kotlin.kotlin.io.output.buffer;"

object NashornJsTestChecker : AbstractNashornJsTestChecker() {

    override fun beforeRun() {
        engine.eval(SETUP_KOTLIN_OUTPUT)
    }

    override val preloadedScripts = listOf(
        TEST_DATA_DIR_PATH + "nashorn-polyfills.js",
        DIST_DIR_JS_PATH + "kotlin.js",
        DIST_DIR_JS_PATH + "kotlin-test.js"
    )

    override fun createScriptEngineForTest(): ScriptEngineNashorn {
        val engine = super.createScriptEngineForTest()

        engine.overrideAsserter()

        return engine
    }
}

object NashornIrJsTestChecker : AbstractNashornJsTestChecker() {
    override val preloadedScripts = listOf(
        TEST_DATA_DIR_PATH + "nashorn-polyfills.js",
        "libraries/stdlib/js-v1/src/js/polyfills.js"
    )
}

object V8JsTestChecker : AbstractJsTestChecker() {
    private val engineTL = object : ThreadLocal<ScriptEngineV8>() {
        override fun initialValue() =
            ScriptEngineV8().apply {
                val preloadedScripts = listOf(
                    DIST_DIR_JS_PATH + "kotlin.js",
                    DIST_DIR_JS_PATH + "kotlin-test.js"
                )
                loadFiles(preloadedScripts)

                overrideAsserter()
            }

        override fun remove() {
            get().release()
        }
    }

    private val engine get() = engineTL.get()

    override fun run(files: List<String>, f: ScriptEngine.() -> String): String {
        engine.eval(SETUP_KOTLIN_OUTPUT)
        return engine.runAndRestoreContext {
            loadFiles(files)
            f()
        }
    }
}

object V8IrJsTestChecker : AbstractJsTestChecker() {
    private val engineTL = object : ThreadLocal<ScriptEngineV8>() {
        override fun initialValue() = ScriptEngineV8()
        override fun remove() {
            get().release()
        }
    }

    override fun run(files: List<String>, f: ScriptEngine.() -> String): String {
        val engine = engineTL.get()
        return try {
            engine.loadFiles(files)
            engine.f()
        } finally {
            engine.reset()
        }
    }
}
