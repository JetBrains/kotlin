/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.engine

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.utils.V8ObjectUtils
import java.io.File

class ScriptEngineV8(LIBRARY_PATH_BASE: String) : ScriptEngine {

    override fun <T> releaseObject(t: T) {
        (t as? V8Object)?.release()
    }

    private var savedState: List<String>? = null

    override fun restoreState() {
        val scriptBuilder = StringBuilder()

        val globalState = getGlobalPropertyNames()
        val originalState = savedState!!
        for (key in globalState) {
            if (key !in originalState) {
                scriptBuilder.append("this['$key'] = void 0;\n")
            }
        }
        evalVoid(scriptBuilder.toString())
    }

    private fun getGlobalPropertyNames(): List<String> {
        val v8Array = eval<V8Array>("Object.getOwnPropertyNames(this)")
        @Suppress("UNCHECKED_CAST") val javaArray = V8ObjectUtils.toList(v8Array) as List<String>
        v8Array.release()
        return javaArray
    }

    override fun saveState() {
        if (savedState == null) {
            savedState = getGlobalPropertyNames()
        }
    }

    private val myRuntime: V8 = V8.createV8Runtime("global", LIBRARY_PATH_BASE)

    @Suppress("UNCHECKED_CAST")
    override fun <T> eval(script: String): T {
        return myRuntime.executeScript(script) as T
    }

    override fun evalVoid(script: String) {
        return myRuntime.executeVoidScript(script)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> callMethod(obj: Any, name: String, vararg args: Any?): T {
        if (obj !is V8Object) {
            throw Exception("InteropV8 can deal only with V8Object")
        }

        val runtimeArray = V8Array(myRuntime)
        val result = obj.executeFunction(name, runtimeArray) as T
        runtimeArray.release()
        return result
    }

    override fun loadFile(path: String) {
        myRuntime.executeVoidScript(File(path).bufferedReader().use { it.readText() }, path, 0)
    }

    override fun release() {
        myRuntime.release()
    }
}

class ScriptEngineV8Lazy(LIBRARY_PATH_BASE: String) : ScriptEngine {
    override fun <T> eval(script: String) = engine.eval<T>(script)

    override fun saveState() = engine.saveState()

    override fun evalVoid(script: String) = engine.evalVoid(script)

    override fun <T> callMethod(obj: Any, name: String, vararg args: Any?) = engine.callMethod<T>(obj, name, args)

    override fun loadFile(path: String) = engine.loadFile(path)

    override fun release() = engine.release()

    override fun <T> releaseObject(t: T) = engine.releaseObject(t)

    override fun restoreState() = engine.restoreState()

    private val engine by lazy { ScriptEngineV8(LIBRARY_PATH_BASE) }
}