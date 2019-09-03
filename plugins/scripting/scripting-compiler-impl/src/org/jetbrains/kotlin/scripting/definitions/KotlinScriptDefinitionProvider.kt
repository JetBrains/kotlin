/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import java.io.File

interface ScriptDefinitionProvider {
    @Deprecated("Migrating to configuration refinement", level = DeprecationLevel.ERROR)
    fun findScriptDefinition(fileName: String): KotlinScriptDefinition?

    @Deprecated("Migrating to configuration refinement", level = DeprecationLevel.ERROR)
    fun getDefaultScriptDefinition(): KotlinScriptDefinition

    fun isScript(file: File): Boolean

    fun findDefinition(file: File): ScriptDefinition?
    fun getDefaultDefinition(): ScriptDefinition

    fun getKnownFilenameExtensions(): Sequence<String>

    companion object {
        fun getInstance(project: Project): ScriptDefinitionProvider? =
            ServiceManager.getService(project, ScriptDefinitionProvider::class.java)
    }
}

