/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsStorage
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsStorage.Companion.ScriptClassRoots
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

internal class GradleClassRootsCache(
    project: Project,
    configuration: Configuration?,
    override val fileToConfiguration: (VirtualFile) -> ScriptCompilationConfigurationWrapper?
) : ScriptClassRootsCache(project, extractRoots(configuration)) {

    override val rootsCacheKey = ScriptClassRootsStorage.Companion.Key("gradle")

    override fun getScriptSdk(file: VirtualFile): Sdk? {
        return firstScriptSdk
    }

    override val firstScriptSdk: Sdk? = configuration?.let { getScriptSdk(configuration.context.javaHome) }

    // TODO what should we do if no configuration is loaded yet
    override fun contains(file: VirtualFile): Boolean = true

    companion object {
        fun extractRoots(configuration: Configuration?): ScriptClassRoots {
            if (configuration == null) {
                return ScriptClassRootsStorage.EMPTY
            }
            val scriptSdk = getScriptSdk(configuration.context.javaHome)
            if (scriptSdk != null && !scriptSdk.isAlreadyIndexed(configuration.context.project)) {
                return ScriptClassRootsStorage.Companion.ScriptClassRoots(
                    configuration.classFilePath,
                    configuration.sourcePath,
                    setOf(scriptSdk)
                )
            }
            return ScriptClassRoots(
                configuration.classFilePath,
                configuration.sourcePath,
                getScriptSdk(configuration.context.javaHome)?.let { setOf(it) } ?: setOf()
            )
        }
    }
}