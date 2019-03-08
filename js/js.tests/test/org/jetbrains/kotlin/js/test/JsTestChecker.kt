/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import jdk.nashorn.internal.runtime.ScriptRuntime
import org.jetbrains.kotlin.js.test.interop.GlobalRuntimeContext
import org.jetbrains.kotlin.js.test.interop.ScriptEngine
import org.jetbrains.kotlin.js.test.interop.ScriptEngineNashorn
import org.jetbrains.kotlin.js.test.interop.ScriptEngineV8
import org.junit.Assert

fun createScriptEngine(): ScriptEngine {
    return ScriptEngineNashorn()
}

fun ScriptEngine.overrideAsserter() {
    evalVoid("this['kotlin-test'].kotlin.test.overrideAsserter_wbnzx$(this['kotlin-test'].kotlin.test.DefaultAsserter);")
}

fun ScriptEngine.runTestFunction(
    testModuleName: String?,
    testPackageName: String?,
    testFunctionName: String,
    withModuleSystem: Boolean
): String? {
    var script = when {
        withModuleSystem -> BasicBoxTest.KOTLIN_TEST_INTERNAL + ".require('" + testModuleName!! + "')"
        testModuleName === null -> "this"
        else -> testModuleName
    }

    if (testPackageName !== null) {
        script += ".$testPackageName"
    }

    val testPackage = eval<Any>(script)
    return callMethod<String?>(testPackage, testFunctionName).also {
        releaseObject(testPackage)
    }
}

abstract class AbstractJsTestChecker {
    fun check(
        files: List<String>,
        testModuleName: String?,
        testPackageName: String?,
        testFunctionName: String,
        expectedResult: String,
        withModuleSystem: Boolean
    ) {
        val actualResult = run(files, testModuleName, testPackageName, testFunctionName, withModuleSystem)
        Assert.assertEquals(expectedResult, actualResult)
    }

    private fun run(
        files: List<String>,
        testModuleName: String?,
        testPackageName: String?,
        testFunctionName: String,
        withModuleSystem: Boolean
    ) = run(files) {
        runTestFunction(testModuleName, testPackageName, testFunctionName, withModuleSystem)
    }

    protected abstract fun run(files: List<String>, f: ScriptEngine.() -> Any?): Any?
}

fun ScriptEngine.runAndRestoreContext(
    globalObject: GlobalRuntimeContext = getGlobalContext(),
    originalState: Map<String, Any?> = globalObject.toMap(),
    f: ScriptEngine.() -> Any?
): Any? {
    return try {
        this.f()
    } finally {
        for (key in globalObject.keys) {
            globalObject[key] = originalState[key] ?: ScriptRuntime.UNDEFINED
        }
    }
}

abstract class AbstractNashornJsTestChecker: AbstractJsTestChecker() {

    private var engineUsageCnt = 0

    private var engineCache: ScriptEngine? = null
    private var globalObject: GlobalRuntimeContext? = null
    private var originalState: Map<String, Any?>? = null

    protected val engine
        get() = engineCache ?: createScriptEngineForTest().also {
            engineCache = it
            globalObject = it.getGlobalContext()
            originalState = globalObject?.toMap()
        }

    fun run(files: List<String>) {
        run(files) { null }
    }

    protected open fun beforeRun() {}

    override fun run(
        files: List<String>,
        f: ScriptEngine.() -> Any?
    ): Any? {
        // Recreate the engine once in a while
        if (engineUsageCnt++ > 100) {
            engineUsageCnt = 0
            engineCache = null
        }

        beforeRun()

        return engine.runAndRestoreContext(globalObject!!, originalState!!) {
            files.forEach(engine::loadFile)
            engine.f()
        }
    }

    protected abstract fun createScriptEngineForTest(): ScriptEngine
}

object NashornJsTestChecker : AbstractNashornJsTestChecker() {
    const val SETUP_KOTLIN_OUTPUT = "kotlin.kotlin.io.output = new kotlin.kotlin.io.BufferedOutput();"
    private const val GET_KOTLIN_OUTPUT = "kotlin.kotlin.io.output.buffer;"

    override fun beforeRun() {
        engine.evalVoid(SETUP_KOTLIN_OUTPUT)
    }

    fun checkStdout(files: List<String>, expectedResult: String) {
        run(files)
        val actualResult = engine.eval<String>(GET_KOTLIN_OUTPUT)
        Assert.assertEquals(expectedResult, actualResult)
    }

    override fun createScriptEngineForTest(): ScriptEngine {
        val engine = createScriptEngine()

        listOf(
            BasicBoxTest.TEST_DATA_DIR_PATH + "nashorn-polyfills.js",
            BasicBoxTest.DIST_DIR_JS_PATH + "kotlin.js",
            BasicBoxTest.DIST_DIR_JS_PATH + "kotlin-test.js"
        ).forEach(engine::loadFile)

        engine.overrideAsserter()

        return engine
    }
}

class NashornIrJsTestChecker : AbstractNashornJsTestChecker() {
    override fun createScriptEngineForTest(): ScriptEngine {
        val engine = createScriptEngine()

        listOfNotNull(
            BasicBoxTest.TEST_DATA_DIR_PATH + "nashorn-polyfills.js",
            "libraries/stdlib/js/src/js/polyfills.js"
        ).forEach(engine::loadFile)

        return engine
    }
}

object V8IrJsTestChecker : AbstractJsTestChecker() {
    override fun run(files: List<String>, f: ScriptEngine.() -> Any?): Any? {

        val v8 = ScriptEngineV8()

        val v = try {
            files.forEach { v8.loadFile(it) }
            v8.f()
        } catch (t: Throwable) {
            try {
                v8.release()
            } finally {
                // Don't mask the original exception
                throw t
            }
        }
        v8.release()

        return v
    }
}