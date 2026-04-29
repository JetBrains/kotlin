/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import org.jetbrains.kotlin.extensions.ExtensionPointDescriptor
import kotlin.script.experimental.api.SourceCode

interface ScriptDefinitionProvider {
    val currentDefinitions: Sequence<ScriptDefinition>

    fun isScript(script: SourceCode): Boolean

    fun findDefinition(script: SourceCode): ScriptDefinition?
    fun getDefaultDefinition(): ScriptDefinition

    fun getKnownFilenameExtensions(): Sequence<String>

    companion object : ExtensionPointDescriptor<ScriptDefinitionProvider>(
        "org.jetbrains.kotlin.scriptDefinitionProvider",
        ScriptDefinitionProvider::class.java
    )
}
