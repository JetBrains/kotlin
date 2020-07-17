/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.core.script.configuration.CompositeScriptConfigurationManager
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.makeFailureResult

// NOTE: this service exists exclusively because ScriptDependencyManager
// cannot be registered as implementing two services (state would be duplicated)
class IdeScriptDependenciesProvider(project: Project) : ScriptDependenciesProvider(project) {
    override fun getScriptConfigurationResult(file: KtFile): ScriptCompilationConfigurationResult? {
        val configuration = getScriptConfiguration(file)
        val reports = IdeScriptReportSink.getReports(file)
        if (configuration == null && reports.isNotEmpty()) {
            return makeFailureResult(reports)
        }
        return configuration?.asSuccess(reports)
    }

    override fun getScriptConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper? {
        return ScriptConfigurationManager.getInstance(project).getConfiguration(file)
    }

}

/**
 * Facade for loading and caching Kotlin script files configuration.
 *
 * This service also starts indexing of new dependency roots and runs highlighting
 * of opened files when configuration will be loaded or updated.
 */
interface ScriptConfigurationManager {
    fun loadPlugins()

    /**
     * Get cached configuration for [file] or load it.
     * May return null even configuration was loaded but was not yet applied.
     */
    fun getConfiguration(file: KtFile): ScriptCompilationConfigurationWrapper?

    @Deprecated("Use getScriptClasspath(KtFile) instead")
    fun getScriptClasspath(file: VirtualFile): List<VirtualFile>

    /**
     * @see [getConfiguration]
     */
    fun getScriptClasspath(file: KtFile): List<VirtualFile>

    /**
     * Check if configuration is already cached for [file] (in cache or FileAttributes).
     * The result may be true, even cached configuration is considered out-of-date.
     *
     * Supposed to be used to switch highlighting off for scripts without configuration
     * to avoid all file being highlighted in red.
     */
    fun hasConfiguration(file: KtFile): Boolean

    /**
     * returns true when there is no configuration and highlighting should be suspended
     */
    fun isConfigurationLoadingInProgress(file: KtFile): Boolean

    /**
     * Update caches that depends on script definitions and do update if necessary
     */
    fun updateScriptDefinitionReferences()

    ///////////////
    // classpath roots info:

    fun getScriptSdk(file: VirtualFile): Sdk?
    fun getFirstScriptsSdk(): Sdk?

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope

    fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope
    fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope
    fun getAllScriptsDependenciesClassFiles(): List<VirtualFile>
    fun getAllScriptDependenciesSources(): List<VirtualFile>

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ScriptConfigurationManager =
            ServiceManager.getService(project, ScriptConfigurationManager::class.java)

        fun toVfsRoots(roots: Iterable<File>): List<VirtualFile> {
            return roots.mapNotNull { classpathEntryToVfs(it) }
        }

        fun classpathEntryToVfs(file: File): VirtualFile? {
            val res = when {
                !file.exists() -> null
                file.isDirectory -> StandardFileSystems.local()?.findFileByPath(file.canonicalPath)
                file.isFile -> StandardFileSystems.jar()?.findFileByPath(file.canonicalPath + URLUtil.JAR_SEPARATOR)
                else -> null
            }
            // TODO: report this somewhere, but do not throw: assert(res != null, { "Invalid classpath entry '$this': exists: ${exists()}, is directory: $isDirectory, is file: $isFile" })
            return res
        }

        @TestOnly
        fun updateScriptDependenciesSynchronously(file: PsiFile) {
            // TODO: review the usages of this method
            (getInstance(file.project) as CompositeScriptConfigurationManager).default
                .updateScriptDependenciesSynchronously(file)
        }

        @TestOnly
        fun clearCaches(project: Project) {
            (getInstance(project) as CompositeScriptConfigurationManager).default
                .updateScriptDefinitionsReferences()
        }

        fun clearManualConfigurationLoadingIfNeeded(file: VirtualFile) {
            if (file.LOAD_CONFIGURATION_MANUALLY == true) {
                file.LOAD_CONFIGURATION_MANUALLY = null
            }
        }

        fun markFileWithManualConfigurationLoading(file: VirtualFile) {
            file.LOAD_CONFIGURATION_MANUALLY = true
        }

        fun isManualConfigurationLoading(file: VirtualFile): Boolean = file.LOAD_CONFIGURATION_MANUALLY ?: false

        private var VirtualFile.LOAD_CONFIGURATION_MANUALLY: Boolean? by UserDataProperty(Key.create<Boolean>("MANUAL_CONFIGURATION_LOADING"))
    }
}
