/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.engine

interface ScriptEngine {
    fun eval(script: String): String

    // TODO Add API to load few files at once?
    fun loadFile(path: String)

    /**
     * Performs truly reset of the engine state.
     * */
    fun reset()

    /**
     * Saves current state of global object.
     *
     * See also [restoreGlobalState]
     */
    fun saveGlobalState()

    /**
     * Restores global object from the last saved state.
     *
     * See also [saveGlobalState]
     */
    fun restoreGlobalState()


    /**
     * Release held resources.
     *
     * Must be called explicitly before an object is garbage collected to avoid leaking resources.
     */
    fun release()
}

interface ScriptEngineWithTypedResult : ScriptEngine {
    fun <R> evalWithTypedResult(script: String): R
}

fun ScriptEngine.loadFiles(files: List<String>) {
    files.forEach { loadFile(it) }
}
