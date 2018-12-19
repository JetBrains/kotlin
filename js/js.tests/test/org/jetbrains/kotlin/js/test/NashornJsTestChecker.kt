/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ScriptRuntime
import org.junit.Assert
import javax.script.Invocable
import javax.script.ScriptEngine

fun createScriptEngine(): ScriptEngine =
    // TODO use "-strict"
    NashornScriptEngineFactory().getScriptEngine("--language=es5", "--no-java", "--no-syntax-extensions")

fun ScriptEngine.overrideAsserter() {
    eval("this['kotlin-test'].kotlin.test.overrideAsserter_wbnzx$(this['kotlin-test'].kotlin.test.DefaultAsserter);")
}

fun ScriptEngine.runTestFunction(
    testModuleName: String?,
    testPackageName: String?,
    testFunctionName: String,
    withModuleSystem: Boolean
): Any? {
    val testModule =
        when {
            withModuleSystem ->
                eval(BasicBoxTest.KOTLIN_TEST_INTERNAL + ".require('" + testModuleName!! + "')")
            testModuleName === null ->
                eval("this")
            else ->
                get(testModuleName)
        }
    testModule as ScriptObjectMirror

    val testPackage =
        when {
            testPackageName === null ->
                testModule
            testPackageName.contains(".") ->
                testPackageName.split(".").fold(testModule) { p, part -> p[part] as ScriptObjectMirror }
            else ->
                testModule[testPackageName]!!
        }

    return (this as Invocable).invokeMethod(testPackage, testFunctionName)
}

fun ScriptEngine.loadFile(path: String) {
    eval("load('${path.replace('\\', '/')}');")
}

fun ScriptEngine.runAndRestoreContext(
    globalObject: ScriptObjectMirror = eval("this") as ScriptObjectMirror,
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

abstract class AbstractNashornJsTestChecker {

    private var engineUsageCnt = 0

    private var engineCache: ScriptEngine? = null
    private var globalObject: ScriptObjectMirror? = null
    private var originalState: Map<String, Any?>? = null

    protected val engine
        get() = engineCache ?: createScriptEngineForTest().also {
            engineCache = it
            globalObject = it.eval("this") as ScriptObjectMirror
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
        engine.eval(SETUP_KOTLIN_OUTPUT)
    }

    fun checkStdout(files: List<String>, expectedResult: String) {
        run(files)
        val actualResult = engine.eval(GET_KOTLIN_OUTPUT)
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

object NashornIrJsTestChecker : AbstractNashornJsTestChecker() {
    override fun createScriptEngineForTest(): ScriptEngine {
        val engine = createScriptEngine()

        listOf(
            BasicBoxTest.TEST_DATA_DIR_PATH + "nashorn-polyfills.js",
            "libraries/stdlib/js/src/js/polyfills.js",
            "js/js.translator/testData/out/irBox/testRuntime.js"
        ).forEach(engine::loadFile)

        return engine
    }
}
