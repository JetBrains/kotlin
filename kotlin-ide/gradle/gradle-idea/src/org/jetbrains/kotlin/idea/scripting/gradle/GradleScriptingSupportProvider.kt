/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.CompositeScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslGradleBuildSync
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.settings.*
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Paths

/**
 * Manages [roots] gradle build roots:
 * - populated after Gradle project sync, by calling [update]
 * - stored in FS and loaded in [init]
 */
class GradleScriptingSupportProvider(val project: Project) : ScriptingSupport.Provider() {
    val manager: CompositeScriptConfigurationManager
        get() = ScriptConfigurationManager.getInstance(project) as CompositeScriptConfigurationManager

    private val updater
        get() = manager.updater

    val roots = RootsIndex()

    ////////////
    /// ScriptingSupport.Provider implementation:

    override fun updateScriptDefinitions() {
        // nothing related to script definition and project roots are cached
    }

    override fun isApplicable(file: VirtualFile): Boolean {
        val buildRoot = findScriptBuildRoot(file) ?: return false
        if (buildRoot is GradleBuildRoot.Legacy) return false
        if (buildRoot is GradleBuildRoot.UnlinkedLegacy) return false
        return true
    }

    override fun isConfigurationLoadingInProgress(file: KtFile): Boolean {
        return when (val root = findScriptBuildRoot(file.originalFile.virtualFile)) {
            is GradleBuildRoot.Linked -> root.importing
            else -> false
        }
    }

    override fun collectConfigurations(builder: ScriptClassRootsCache.Builder) {
        roots.values.forEach { root ->
            if (root is GradleBuildRoot.Imported) {
                root.collectConfigurations(builder)
            }
        }
    }

    //////////////////

    private val VirtualFile.localPath
        get() = path

    fun getScriptInfo(file: VirtualFile): GradleBuildRoot.Imported.ScriptInfo? =
        manager.getLightScriptInfo(file.localPath) as? GradleBuildRoot.Imported.ScriptInfo

    fun findScriptBuildRoot(file: VirtualFile): GradleBuildRoot? {
        if (!isGradleKotlinScript(file)) return null

        val path = file.localPath
        val found = roots.findNearestRoot(path)
        if (found != null) return found

        return findNewScriptBuildRoot(path)
    }

    private fun findNewScriptBuildRoot(scriptPath: String): GradleBuildRoot {
        val newRoot = updater.update {
            loadScriptBuildRoot(scriptPath)
        }
        roots[newRoot.pathPrefix] = newRoot
        return newRoot
    }

    private fun updateBuildRoot(rootPath: String) {
        val settings = getGradleProjectSettings(rootPath) ?: return
        val newRoot = loadLinkedRoot(settings)
        roots[newRoot.pathPrefix] = newRoot
        if (newRoot is GradleBuildRoot.Imported) {
            updater.ensureUpdateScheduled()
        }
    }

    private fun loadScriptBuildRoot(scriptPath: String): GradleBuildRoot {
        val settings = findNearestGradleProjectSettings(scriptPath) ?: return detectUnlinkedGradleBuildRoot(scriptPath)
        return loadLinkedRoot(settings)
    }

    private fun loadLinkedRoot(settings: GradleProjectSettings) =
        tryLoadFromFsCache(settings) ?: createOtherLinkedRoot(settings)

    private fun createOtherLinkedRoot(settings: GradleProjectSettings): GradleBuildRoot.Linked {
        val supported = kotlinDslScriptsModelImportSupported(settings.resolveGradleVersion().version)
        return when {
            supported -> GradleBuildRoot.New(settings.externalProjectPath)
            else -> GradleBuildRoot.Legacy(settings.externalProjectPath)
        }
    }

    private fun getGradleProjectSettings(workingDir: String): GradleProjectSettings? {
        return (ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID) as GradleSettings)
            .getLinkedProjectSettings(workingDir)
    }

    private fun findNearestGradleProjectSettings(localPath: String): GradleProjectSettings? {
        return getGradleProjectSettings(project)
            .filter { localPath.startsWith(it.externalProjectPath) }
            .maxBy { it.externalProjectPath.length }
    }

    private fun detectUnlinkedGradleBuildRoot(file: String): GradleBuildRoot.Unlinked =
        GradleBuildRoot.UnlinkedUnknown(file)

    init {
        getGradleProjectSettings(project).forEach {
            roots[it.externalProjectPath] = loadLinkedRoot(it)
        }

        // subscribe to gradle build unlink
        val listener = object : GradleSettingsListenerAdapter() {
            override fun onProjectsLinked(settings: MutableCollection<GradleProjectSettings>) {
                settings.forEach {
                    loadLinkedRoot(it)
                }
            }

            override fun onProjectsUnlinked(linkedProjectPaths: MutableSet<String>) {
                linkedProjectPaths.forEach {
                    val buildRoot = VfsUtil.findFile(Paths.get(it), false)
                    if (buildRoot != null) {
                        if (roots.remove(buildRoot.localPath) != null) {
                            KotlinDslScriptModels.remove(buildRoot)
                        }
                    }
                }
            }

            override fun onGradleHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String) {
                updateBuildRoot(linkedProjectPath)
                manager.updater.ensureUpdateScheduled()
            }

            override fun onGradleDistributionTypeChange(currentValue: DistributionType?, linkedProjectPath: String) {
                updateBuildRoot(linkedProjectPath)
                manager.updater.ensureUpdateScheduled()
            }
        }

        project.messageBus.connect(project).subscribe(GradleSettingsListener.TOPIC, listener)
    }

    private fun tryLoadFromFsCache(settings: GradleProjectSettings) =
        tryCreateImportedRoot(settings.externalProjectPath) {
            KotlinDslScriptModels.read(it)
        }

    fun markImportingInProgress(workingDir: String) {
        val linked = roots.getBuildRoot(workingDir) as? GradleBuildRoot.Linked
        if (linked != null) {
            linked.importing = true
        } else {
            val settings = getGradleProjectSettings(workingDir)
            if (settings != null) {
                val root = createOtherLinkedRoot(settings)
                root.importing = true
                roots[workingDir] = root
            }
        }
    }

    fun update(build: KotlinDslGradleBuildSync) {
        val settings = getGradleProjectSettings(build.workingDir) ?: return

        (roots.getBuildRoot(build.workingDir) as? GradleBuildRoot.Linked)?.importing = false

        if (!kotlinDslScriptsModelImportSupported(settings.resolveGradleVersion().version)) {
            roots[build.workingDir] = GradleBuildRoot.Legacy(build.workingDir)
            return
        }

        // fast path for linked gradle builds without .gradle.kts support
        if (build.models.isEmpty()) {
            val root = roots.findNearestRoot(build.workingDir) ?: return
            if (root is GradleBuildRoot.Imported && root.data.models.isEmpty()) return
        }

        val templateClasspath = findTemplateClasspath(build) ?: return
        val data = GradleImportedBuildRootData(templateClasspath, build.models)
        val newSupport = tryCreateImportedRoot(build.workingDir) { data } ?: return
        KotlinDslScriptModels.write(newSupport.dir, data)
        roots[build.workingDir] = newSupport
        manager.updater.ensureUpdateScheduled()
    }

    private fun tryCreateImportedRoot(
        externalProjectPath: String,
        dataProvider: (buildRoot: VirtualFile) -> GradleImportedBuildRootData?
    ): GradleBuildRoot.Imported? {
        val gradleExeSettings =
            ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
                project,
                externalProjectPath,
                GradleConstants.SYSTEM_ID
            )

        val buildRoot = VfsUtil.findFile(Paths.get(externalProjectPath), true) ?: return null
        val data = dataProvider(buildRoot) ?: return null

        val newSupport = GradleBuildRoot.Imported(
            project,
            buildRoot,
            GradleKtsContext(gradleExeSettings.javaHome?.let { File(it) }),
            data
        )

        val oldSupport = roots.findNearestRoot(externalProjectPath)
        if (oldSupport != null) {
            hideNotificationForProjectImport(project)
        }

        return newSupport
    }

    private fun findTemplateClasspath(build: KotlinDslGradleBuildSync): List<String>? {
        val anyScript = VfsUtil.findFile(Paths.get(build.models.first().file), true)!!
        // todo: find definition according to build.workingDir
        val definition = anyScript.findScriptDefinition(project) ?: return null
        return definition.asLegacyOrNull<KotlinScriptDefinitionFromAnnotatedTemplate>()
            ?.templateClasspath?.map { it.path }
    }

    // used in 201
    @Suppress("UNUSED")
    fun isConfigurationOutOfDate(file: VirtualFile): Boolean {
        val script = getScriptInfo(file) ?: return false
        return script.model.inputs.isUpToDate(project, file)
    }

    companion object {
        fun getInstance(project: Project): GradleScriptingSupportProvider =
            EPN.getPoint(project).extensionList.firstIsInstance()
    }
}
