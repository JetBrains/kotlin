/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import java.io.File
import kotlin.script.experimental.intellij.ScriptDefinitionsProvider

class MainKtsScriptDefinitionSource : ScriptDefinitionsProvider {
    override val id: String = ".main.kts script"

    override fun getDefinitionClasses(): Iterable<String> = emptyList()

    override fun getDefinitionsClassPath(): Iterable<File> {
        return with(KotlinArtifacts.instance) {
            listOf(kotlinMainKts, kotlinScriptRuntime, kotlinStdlib, kotlinReflect)
        }
    }

    override fun useDiscovery(): Boolean = true
}