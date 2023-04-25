/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.project.Project
import kotlin.script.experimental.api.SourceCode

interface ScriptDefinitionProvider {
    @Deprecated("Migrating to configuration refinement", level = DeprecationLevel.ERROR)
    fun findScriptDefinition(fileName: String): KotlinScriptDefinition?

    @Deprecated("Migrating to configuration refinement", level = DeprecationLevel.ERROR)
    fun getDefaultScriptDefinition(): KotlinScriptDefinition

    fun isScript(script: SourceCode): Boolean

    fun findDefinition(script: SourceCode): ScriptDefinition?
    fun getDefaultDefinition(): ScriptDefinition

    fun getKnownFilenameExtensions(): Sequence<String>

    companion object {
        fun getInstance(project: Project): ScriptDefinitionProvider? =
            project.getService(ScriptDefinitionProvider::class.java)

        fun getServiceIfCreated(project: Project): ScriptDefinitionProvider? =
            project.getServiceIfCreated(ScriptDefinitionProvider::class.java)
    }
}
