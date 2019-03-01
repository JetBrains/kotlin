/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.js.test.interop.*
import org.junit.Assert

private val USE_J2V8_INTEROP_FOR_JS_TESTS = java.lang.Boolean.getBoolean("org.jetbrains.kotlin.use.j2v8.interop.for.js.tests")

fun createScriptEngine(): InteropEngine {
    return if (USE_J2V8_INTEROP_FOR_JS_TESTS) {
        InteropV8()
    } else {
        InteropNashorn()
    }
}

fun InteropEngine.overrideAsserter() {
    evalVoid("this['kotlin-test'].kotlin.test.overrideAsserter_wbnzx$(this['kotlin-test'].kotlin.test.DefaultAsserter);")
}

fun InteropEngine.runTestFunction(
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

    return callMethod(eval(script), testFunctionName)
}

fun InteropEngine.runAndRestoreContext(
    globalObject: InteropGlobalContext = getGlobalContext(),
    originalState: Map<String, Any?> = globalObject.toMap(),
    f: InteropEngine.() -> Any?
): Any? {
    return try {
        this.f()
    } finally {
        globalObject.updateState(originalState)
    }
}

abstract class AbstractNashornJsTestChecker {

    private var engineUsageCnt = 0

    private var engineCache: InteropEngine? = null
    private var globalObject: InteropGlobalContext? = null
    private var originalState: Map<String, Any?>? = null

    protected val engine
        get() = engineCache ?: createScriptEngineForTest().also {
            engineCache = it
            globalObject = it.getGlobalContext()
            originalState = globalObject?.toMap()
        }

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

    fun run(files: List<String>) {
        run(files) { null }
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

    protected open fun beforeRun() {}

    private fun run(
        files: List<String>,
        f: InteropEngine.() -> Any?
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

    protected abstract fun createScriptEngineForTest(): InteropEngine
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

    override fun createScriptEngineForTest(): InteropEngine {
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

class NashornIrJsTestChecker(private val runtime: JsIrTestRuntime) : AbstractNashornJsTestChecker() {
    override fun createScriptEngineForTest(): InteropEngine {
        val engine = createScriptEngine()

        listOf(
            BasicBoxTest.TEST_DATA_DIR_PATH + "nashorn-polyfills.js",
            "libraries/stdlib/js/src/js/polyfills.js",
            runtime.path
        ).forEach(engine::loadFile)

        return engine
    }
}

val nashornIrJsTestCheckers = JsIrTestRuntime.values().associate {
    it to NashornIrJsTestChecker(it)
}
