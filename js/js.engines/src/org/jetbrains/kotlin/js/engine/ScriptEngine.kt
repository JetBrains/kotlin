/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.engine

interface ScriptEngine {
    fun <T> eval(script: String): T
    fun evalVoid(script: String)
    fun <T> callMethod(obj: Any, name: String, vararg args: Any?): T
    fun loadFile(path: String)
    fun release()
    fun <T> releaseObject(t: T)

    fun saveState()
    fun restoreState()
}