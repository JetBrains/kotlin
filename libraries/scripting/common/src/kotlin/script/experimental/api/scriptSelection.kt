/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

interface ScriptSelector {

    val name: String

    val fileExtension: String // for preliminary selection by file type, e.g. in ide

    fun makeScriptName(scriptFileName: String?): String

    fun isKnownScript(script: ScriptSource): Boolean
}
