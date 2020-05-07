/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.CompositeScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.scripting.gradle.*
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslGradleBuildSync
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.settings.*
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [GradleBuildRoot] is a linked gradle build (don't confuse with gradle project and included build).
 * Each [GradleBuildRoot] may have it's own Gradle version, Java home and other settings.
 *
 * Typically, IntelliJ project have no more than one [GradleBuildRoot].
 *
 * This manager allows to find related Gradle build by the Gradle Kotlin script file path.
 * Each imported build have info about all of it's Kotlin Build Scripts.
 * It is populated by calling [update], stored in FS and will be loaded from FS on next project opening
 *
 * [CompositeScriptConfigurationManager] may ask about known scripts by calling [collectConfigurations].
 *
 * It also used to show related notification and floating actions depending on root kind, state and script state itself.
 *
 * Roots may be:
 * - [GradleBuildRoot.Unlinked] - The script not related to any Gradle build that is linked to IntelliJ Project,
 *                                or we cannot known what is it
 * - [GradleBuildRoot.Linked] - Linked project, that may be itself:
 *   - [GradleBuildRoot.Legacy] - Gradle build with old Gradle version (<6.0)
 *   - [GradleBuildRoot.New] - not yet imported
 *   - [GradleBuildRoot.Imported] - imported
 */
class GradleBuildRootsManager(val project: Project) : ScriptingSupport.Provider() {
    private val manager: CompositeScriptConfigurationManager
        get() = ScriptConfigurationManager.getInstance(project) as CompositeScriptConfigurationManager

    private val updater
        get() = manager.updater

    private val roots = GradleBuildRootIndex()

    ////////////
    /// ScriptingSupport.Provider implementation:

    override fun updateScriptDefinitions() {
        // nothing related to script definition and project roots are cached
    }

    override fun isApplicable(file: VirtualFile): Boolean {
        val scriptUnderRoot = findScriptBuildRoot(file) ?: return false
        if (scriptUnderRoot.root is GradleBuildRoot.Legacy) return false
        return true
    }

    override fun isConfigurationLoadingInProgress(file: KtFile): Boolean {
        return when (val root = findScriptBuildRoot(file.originalFile.virtualFile)?.root) {
            is GradleBuildRoot.Linked -> root.importing
            else -> false
        }
    }

    // used in 201
    @Suppress("UNUSED")
    fun isConfigurationOutOfDate(file: VirtualFile): Boolean {
        val script = getScriptInfo(file) ?: return false
        return !script.model.inputs.isUpToDate(project, file)
    }

    override fun collectConfigurations(builder: ScriptClassRootsCache.Builder) {
        roots.list.forEach { root ->
            if (root is GradleBuildRoot.Imported) {
                root.collectConfigurations(builder)
            }
        }
    }

    //////////////////

    private val VirtualFile.localPath
        get() = path

    fun getScriptInfo(file: VirtualFile): GradleScriptInfo? =
        getScriptInfo(file.localPath)

    fun getScriptInfo(localPath: String): GradleScriptInfo? =
        manager.getLightScriptInfo(localPath) as? GradleScriptInfo

    class ScriptUnderRoot(
        val root: GradleBuildRoot?,
        val script: GradleScriptInfo? = null
    )

    fun findScriptBuildRoot(gradleKtsFile: VirtualFile): ScriptUnderRoot? =
        findScriptBuildRoot(gradleKtsFile.path)

    private fun findScriptBuildRoot(filePath: String, searchNearestLegacy: Boolean = true): ScriptUnderRoot? {
        if (!filePath.endsWith(".gradle.kts")) return null

        val scriptInfo = getScriptInfo(filePath)
        val imported = scriptInfo?.buildRoot
        if (imported != null) return ScriptUnderRoot(imported, scriptInfo)

        if (filePath.endsWith("/build.gradle.kts") ||
            filePath.endsWith("/settings.gradle.kts") ||
            filePath.endsWith("/init.gradle.kts")
        ) {
            // build|settings|init.gradle.kts scripts should be located near gradle project root only
            val gradleBuild = roots.getBuildByProjectDir(filePath.substringBeforeLast("/"))
            if (gradleBuild != null) return ScriptUnderRoot(gradleBuild)
        }

        // other scripts: "included", "precompiled" scripts, scripts in unlinked projects,
        // or just random files with ".gradle.kts" ending

        // todo(gradle6): remove, it is required only for projects with old gradle
        if (searchNearestLegacy) {
            val found = roots.findNearestRoot(filePath)
            if (found is GradleBuildRoot.Legacy) return ScriptUnderRoot(found)
        }

        return ScriptUnderRoot(GradleBuildRoot.Unlinked())
    }

    fun isUnderProjectDir(localPath: String) =
        roots.getBuildByProjectDir(localPath) != null

    fun getBuildRoot(gradleWorkingDir: String) =
        roots.getBuildRoot(gradleWorkingDir)

    fun fileChanged(filePath: String, ts: Long = System.currentTimeMillis()) {
        val root = findScriptBuildRoot(filePath, searchNearestLegacy = false)?.root as? GradleBuildRoot.Linked
        root?.fileChanged(filePath, ts)
    }

    fun markImportingInProgress(workingDir: String, inProgress: Boolean = true) {
        actualizeBuildRoot(workingDir)?.importing = inProgress
        updateNotifications(workingDir)
        hideNotificationForProjectImport(project)
    }

    fun update(build: KotlinDslGradleBuildSync) {
        // fast path for linked gradle builds without .gradle.kts support
        if (build.models.isEmpty()) {
            val root = roots.getBuildRoot(build.workingDir) ?: return
            if (root is GradleBuildRoot.Imported && root.data.models.isEmpty()) return
        }

        val root = actualizeBuildRoot(build.workingDir) ?: return
        root.importing = false

        if (root is GradleBuildRoot.Legacy) return

        val templateClasspath = GradleScriptDefinitionsContributor.getDefinitionsTemplateClasspath(project)
        val newData = GradleBuildRootData(build.projectRoots, templateClasspath, build.models)
        val mergedData = if (build.failed && root is GradleBuildRoot.Imported) merge(root.data, newData) else newData

        val newSupport = tryCreateImportedRoot(build.workingDir) { mergedData } ?: return
        GradleBuildRootDataSerializer.write(newSupport.dir, mergedData)

        add(newSupport)

        hideNotificationForProjectImport(project)
    }

    private fun merge(old: GradleBuildRootData, new: GradleBuildRootData): GradleBuildRootData {
        val roots = old.projectRoots.toMutableSet()
        roots.addAll(new.projectRoots)

        val models = old.models.associateByTo(mutableMapOf()) { it.file }
        new.models.associateByTo(models) { it.file }

        return GradleBuildRootData(roots, new.templateClasspath, models.values)
    }

    private val lastModifiedFilesSaveScheduled = AtomicBoolean()

    fun scheduleLastModifiedFilesSave() {
        if (lastModifiedFilesSaveScheduled.compareAndSet(false, true)) {
            BackgroundTaskUtil.executeOnPooledThread(project) {
                if (lastModifiedFilesSaveScheduled.compareAndSet(true, false)) {
                    roots.list.forEach {
                        it.saveLastModifiedFiles()
                    }
                }
            }
        }
    }

    init {
        getGradleProjectSettings(project).forEach {
            // don't call this.add, as we are inside scripting manager initialization
            roots.add(loadLinkedRoot(it))
        }

        // subscribe to linked gradle project modification
        val listener = object : GradleSettingsListenerAdapter() {
            override fun onProjectsLinked(settings: MutableCollection<GradleProjectSettings>) {
                settings.forEach {
                    add(loadLinkedRoot(it))
                }
            }

            override fun onProjectsUnlinked(linkedProjectPaths: MutableSet<String>) {
                linkedProjectPaths.forEach {
                    remove(it)
                }
            }

            override fun onGradleHomeChange(oldPath: String?, newPath: String?, linkedProjectPath: String) {
                reloadBuildRoot(linkedProjectPath)
            }

            override fun onGradleDistributionTypeChange(currentValue: DistributionType?, linkedProjectPath: String) {
                reloadBuildRoot(linkedProjectPath)
            }
        }

        project.messageBus.connect(project).subscribe(GradleSettingsListener.TOPIC, listener)
    }

    private fun getGradleProjectSettings(workingDir: String): GradleProjectSettings? {
        return (ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID) as GradleSettings)
            .getLinkedProjectSettings(workingDir)
    }

    /**
     * Check that root under [workingDir] in sync with it's [GradleProjectSettings].
     * Actually this should be true, but we may miss some change events.
     * For that cases we are rechecking this on each Gradle Project sync (importing/reimporting)
     */
    private fun actualizeBuildRoot(workingDir: String): GradleBuildRoot.Linked? {
        val actualSettings = getGradleProjectSettings(workingDir)
        val buildRoot = roots.getBuildRoot(workingDir)

        return when {
            buildRoot != null -> when {
                !buildRoot.checkActual(actualSettings) -> reloadBuildRoot(workingDir)
                else -> buildRoot
            }
            actualSettings != null -> loadLinkedRoot(actualSettings)
            else -> null
        }
    }

    private fun GradleBuildRoot.Linked.checkActual(actualSettings: GradleProjectSettings?): Boolean {
        if (actualSettings == null) return false

        val knownAsSupported = this !is GradleBuildRoot.Legacy
        val shouldBeSupported = kotlinDslScriptsModelImportSupported(actualSettings.resolveGradleVersion().version)
        return knownAsSupported == shouldBeSupported
    }

    private fun reloadBuildRoot(rootPath: String): GradleBuildRoot.Linked? {
        val settings = getGradleProjectSettings(rootPath)
        if (settings == null) {
            remove(rootPath)
            return null
        } else {
            val newRoot = loadLinkedRoot(settings)
            add(newRoot)
            return newRoot
        }
    }

    private fun removeData(rootPath: String) {
        val buildRoot = LocalFileSystem.getInstance().findFileByPath(rootPath)
        if (buildRoot != null) GradleBuildRootDataSerializer.remove(buildRoot)
    }

    private fun loadLinkedRoot(settings: GradleProjectSettings) =
        tryLoadFromFsCache(settings) ?: createOtherLinkedRoot(settings)

    private fun tryLoadFromFsCache(settings: GradleProjectSettings) =
        tryCreateImportedRoot(settings.externalProjectPath) {
            GradleBuildRootDataSerializer.read(it)
        }

    private fun createOtherLinkedRoot(settings: GradleProjectSettings): GradleBuildRoot.Linked {
        val supported = kotlinDslScriptsModelImportSupported(settings.resolveGradleVersion().version)
        return when {
            supported -> GradleBuildRoot.New(this, settings)
            else -> GradleBuildRoot.Legacy(this, settings)
        }
    }

    private fun tryCreateImportedRoot(
        externalProjectPath: String,
        dataProvider: (buildRoot: VirtualFile) -> GradleBuildRootData?
    ): GradleBuildRoot.Imported? {
        val buildRoot = VfsUtil.findFile(Paths.get(externalProjectPath), true) ?: return null
        val data = dataProvider(buildRoot) ?: return null
        val javaHome = ExternalSystemApiUtil
            .getExecutionSettings<GradleExecutionSettings>(project, externalProjectPath, GradleConstants.SYSTEM_ID)
            .javaHome?.let { File(it) }

        return GradleBuildRoot.Imported(this, buildRoot, javaHome, data)
    }

    private fun add(newRoot: GradleBuildRoot.Linked) {
        val old = roots.add(newRoot)
        if (old is GradleBuildRoot.Imported) removeData(old.pathPrefix)
        if (old is GradleBuildRoot.Imported || newRoot is GradleBuildRoot.Imported) {
            updater.ensureUpdateScheduled()
        }

        updateNotifications(newRoot.pathPrefix)
    }

    private fun remove(rootPath: String) {
        val removed = roots.remove(rootPath)
        if (removed is GradleBuildRoot.Imported) {
            removeData(rootPath)
            updater.ensureUpdateScheduled()
        }

        updateNotifications(rootPath)
    }

    private fun updateNotifications(dir1: String) {
        if (!project.isOpen) return

        val openedScripts = FileEditorManager.getInstance(project).openFiles.filter {
            it.path.startsWith(dir1) && isGradleKotlinScript(it)
        }

        if (openedScripts.isEmpty()) return

        GlobalScope.launch(EDT(project)) {
            if (project.isDisposed) return@launch

            openedScripts.forEach {
                val ktFile = PsiManager.getInstance(project).findFile(it)
                if (ktFile != null) DaemonCodeAnalyzer.getInstance(project).restart(ktFile)
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
        }
    }

    companion object {
        fun getInstance(project: Project): GradleBuildRootsManager =
            EPN.getPoint(project).extensionList.firstIsInstance()
    }
}
