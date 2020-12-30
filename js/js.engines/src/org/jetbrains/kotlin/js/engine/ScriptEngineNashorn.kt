/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.jetbrains.kotlin.js.engine

import jdk.nashorn.api.scripting.NashornScriptEngine
import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import jdk.nashorn.internal.runtime.ScriptRuntime

class ScriptEngineNashorn : ScriptEngineWithTypedResult {
    private var savedState: Map<String, Any?>? = null

    // TODO use "-strict"
    private val myEngine = NashornScriptEngineFactory().getScriptEngine("--language=es5", "--no-java", "--no-syntax-extensions")

    override fun eval(script: String): String = evalWithTypedResult<Any?>(script).toString()

    @Suppress("UNCHECKED_CAST")
    override fun <R> evalWithTypedResult(script: String): R {
        return myEngine.eval(script) as R
    }

    override fun loadFile(path: String) {
        eval("load('${path.replace('\\', '/')}');")
    }

    override fun reset() {
        throw UnsupportedOperationException()
    }

    private fun getGlobalState(): MutableMap<String, Any?> = evalWithTypedResult("this")

    override fun saveGlobalState() {
        savedState = getGlobalState().toMap()
    }

    override fun restoreGlobalState() {
        val globalState = getGlobalState()
        val originalState = savedState!!
        for (key in globalState.keys) {
            globalState[key] = originalState[key] ?: ScriptRuntime.UNDEFINED
        }
    }

    override fun release() {
    }
}
