/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.scripting

open class ScriptingExtension {
    internal val myScriptDefinitions = mutableListOf<String>()
    internal val myScriptDefinitionsClasspath = mutableListOf<String>()
    internal var myDisableScriptDefinitionsFromClasspath = false
    internal val myScriptResolverEnvironment = mutableMapOf<String, String?>()

    open fun scriptDefinition(fqName: String) {
        myScriptDefinitions.add(fqName)
    }

    open fun scriptDefinitions(fqNames: List<String>) {
        myScriptDefinitions.addAll(fqNames)
    }

    open fun scriptDefinitions(vararg fqNames: String) {
        myScriptDefinitions.addAll(fqNames)
    }

    open fun scriptDefinitionsClasspath(vararg paths: String) {
        myScriptDefinitionsClasspath.addAll(paths)
    }

    open fun scriptDefinitionsClasspath(paths: List<String>) {
        myScriptDefinitionsClasspath.addAll(paths)
    }

    open fun disableScriptDefinitionsFromClasspath(disable: Boolean) {
        myDisableScriptDefinitionsFromClasspath = disable
    }

    open fun scriptResolverEnvironment(vararg pairs: Pair<String, String?>) {
        myScriptResolverEnvironment.putAll(pairs)
    }

    open fun scriptResolverEnvironment(pairs: List<Pair<String, String?>>) {
        myScriptResolverEnvironment.putAll(pairs)
    }
}
