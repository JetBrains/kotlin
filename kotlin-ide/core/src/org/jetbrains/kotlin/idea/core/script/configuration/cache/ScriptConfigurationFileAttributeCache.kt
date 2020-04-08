/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.cache

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.idea.core.util.cachedFileAttribute
import org.jetbrains.kotlin.idea.core.util.readObject
import org.jetbrains.kotlin.idea.core.util.writeObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper.FromCompilationConfiguration
import java.io.Serializable
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.jvm.impl.toClassPathOrEmpty

internal class ScriptConfigurationFileAttributeCache(
    val project: Project
) : ScriptConfigurationLoader {
    /**
     * todo(KT-34444): this should be changed to storing all roots in the persistent file cache
     */
    override fun loadDependencies(
        isFirstLoad: Boolean,
        ktFile: KtFile,
        scriptDefinition: ScriptDefinition,
        context: ScriptConfigurationLoadingContext
    ): Boolean {
        if (!isFirstLoad) return false

        val virtualFile = ktFile.originalFile.virtualFile
        val fromFs = load(virtualFile) ?: return false

        context.saveNewConfiguration(
            virtualFile,
            ScriptConfigurationSnapshot(
                fromFs.inputs,
                fromFs.reports,
                FromCompilationConfiguration(KtFileScriptSource(ktFile), fromFs.configuration)
            )
        )
        return fromFs.inputs.isUpToDate(ktFile.project, virtualFile, ktFile)
    }

    private fun load(
        virtualFile: VirtualFile
    ): ScriptConfigurationSnapshotForFS? {
        val configurationSnapshot = virtualFile.scriptConfigurationSnapshot ?: return null
        debug(virtualFile) { "configuration from fileAttributes = $configurationSnapshot" }

        val configuration = configurationSnapshot.configuration ?: return null

        if (!areDependenciesValid(virtualFile, configuration)) {
            save(virtualFile, null)
            return null
        }

        return configurationSnapshot
    }

    private fun areDependenciesValid(file: VirtualFile, configuration: ScriptCompilationConfiguration): Boolean {
        val classpath = configuration.get(ScriptCompilationConfiguration.dependencies).toClassPathOrEmpty()
        return classpath.all {
            if (it.exists()) {
                true
            } else {
                debug(file) {
                    "classpath root saved to file attribute doesn't exist: ${it.path}"
                }
                false
            }
        }
    }

    fun save(file: VirtualFile, value: ScriptConfigurationSnapshot?) {
        if (value == null) {
            file.scriptConfigurationSnapshot = null
        } else {
            file.scriptConfigurationSnapshot = ScriptConfigurationSnapshotForFS(
                value.inputs,
                value.reports,
                value.configuration?.configuration
            )
        }
    }
}

private class ScriptConfigurationSnapshotForFS(
    val inputs: CachedConfigurationInputs,
    val reports: List<ScriptDiagnostic>,
    val configuration: ScriptCompilationConfiguration?
) : Serializable

private var VirtualFile.scriptConfigurationSnapshot: ScriptConfigurationSnapshotForFS? by cachedFileAttribute(
    name = "kotlin-script-dependencies",
    version = 5,
    read = { readObject<ScriptConfigurationSnapshotForFS>() },
    write = { writeObject<ScriptConfigurationSnapshotForFS>(it) }
)
