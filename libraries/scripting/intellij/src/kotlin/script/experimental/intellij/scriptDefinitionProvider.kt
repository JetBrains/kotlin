/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.intellij

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import java.io.File

interface ScriptDefinitionsProvider {
    val id: String

    fun getDefinitionClasses(): Iterable<String>

    fun getDefinitionsClassPath(): Iterable<File>

    fun useDiscovery(): Boolean

    companion object {
        val EP_NAME: ExtensionPointName<ScriptDefinitionsProvider> =
            ExtensionPointName.create<ScriptDefinitionsProvider>("org.jetbrains.kotlin.scriptDefinitionsProvider")
    }

}
