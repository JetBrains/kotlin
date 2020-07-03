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
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport.Companion.EPN
import org.jetbrains.kotlin.idea.core.script.ucache.ScriptClassRootsBuilder
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.scripting.gradle.*
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslGradleBuildSync
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRoot.ImportingStatus.*
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter
import org.jetbrains.plugins.gradle.service.GradleInstallationManager
import org.jetbrains.plugins.gradle.settings.*
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentLinkedQueue
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
 * - [GradleBuildRoot] - Linked project, that may be itself:
 *   - [Legacy] - Gradle build with old Gradle version (<6.0)
 *   - [New] - not yet imported
 *   - [Imported] - imported
 */
class GradleBuildRootsManager(val project: Project) : GradleBuildRootsLocator(), ScriptingSupport {
    private val manager: CompositeScriptConfigurationManager
        get() = ScriptConfigurationManager.getInstance(project) as CompositeScriptConfigurationManager

    private val updater
        get() = manager.updater

    var enabled: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                roots.list.toList().forEach {
                    reloadBuildRoot(it.pathPrefix, null)
                }
            }
        }

    ////////////
    /// ScriptingSupport.Provider implementation:

    override fun isApplicable(file: VirtualFile): Boolean {
        val scriptUnderRoot = findScriptBuildRoot(file) ?: return false
        if (scriptUnderRoot.nearest is Legacy) return false
        if (roots.isStandaloneScript(file.path)) return false
        return true
    }

    override fun isConfigurationLoadingInProgress(file: KtFile): Boolean {
        return findScriptBuildRoot(file.originalFile.virtualFile)?.nearest?.isImportingInProgress() ?: return false
    }

    @Suppress("MemberVisibilityCanBePrivate") // used in GradleImportHelper.kt.201
    fun isConfigurationOutOfDate(file: VirtualFile): Boolean {
        val script = getScriptInfo(file) ?: return false
        if (script.buildRoot.isImportingInProgress()) return false
        return !script.model.inputs.isUpToDate(project, file)
    }

    override fun collectConfigurations(builder: ScriptClassRootsBuilder) {
        roots.list.forEach { root ->
            if (root is Imported) {
                root.collectConfigurations(builder)
            }
        }
    }

    override fun afterUpdate() {
        roots.list.forEach { root ->
            if (root.importing.compareAndSet(updatingCaches, updated)) {
                updateNotifications { it.startsWith(root.pathPrefix) }
            }
        }
    }

    //////////////////

    override fun getScriptInfo(localPath: String): GradleScriptInfo? =
        manager.getLightScriptInfo(localPath) as? GradleScriptInfo

    override fun getScriptFirstSeenTs(path: String): Long {
        val nioPath = FileSystems.getDefault().getPath(path)
        return Files.readAttributes(nioPath, BasicFileAttributes::class.java)
            ?.creationTime()?.toMillis()
            ?: Long.MAX_VALUE
    }

    fun fileChanged(filePath: String, ts: Long = System.currentTimeMillis()) {
        findAffectedFileRoot(filePath)?.fileChanged(filePath, ts)
        scheduleModifiedFilesCheck(filePath)
    }

    fun markImportingInProgress(workingDir: String, inProgress: Boolean = true) {
        actualizeBuildRoot(workingDir, null)?.importing?.set(if (inProgress) importing else updated)
        updateNotifications { it.startsWith(workingDir) }
    }

    fun update(sync: KotlinDslGradleBuildSync) {
        val oldRoot = actualizeBuildRoot(sync.workingDir, sync.gradleVersion) ?: return

        try {
            val newRoot = updateRoot(oldRoot, sync)
            if (newRoot == null) {
                markImportingInProgress(sync.workingDir, false)
                return
            }

            add(newRoot)
        } catch (e: Exception) {
            markImportingInProgress(sync.workingDir, false)
            return
        }
    }

    private fun updateRoot(oldRoot: GradleBuildRoot, sync: KotlinDslGradleBuildSync): Imported? {
        // fast path for linked gradle builds without .gradle.kts support
        if (sync.models.isEmpty()) {
            if (oldRoot is Imported && oldRoot.data.models.isEmpty()) return null
        }

        if (oldRoot is Legacy) return null

        scriptingDebugLog { "gradle project info after import: $sync" }

        // TODO: can gradleHome be null, what to do in this case
        val gradleHome = sync.gradleHome
        if (gradleHome == null) {
            scriptingInfoLog("Cannot find valid gradle home for ${sync.gradleHome} with version = ${sync.gradleVersion}, script models cannot be saved")
            return null
        }

        oldRoot.importing.set(updatingCaches)

        scriptingDebugLog { "save script models after import: ${sync.models}" }

        val newData = GradleBuildRootData(sync.ts, sync.projectRoots, gradleHome, sync.javaHome, sync.models)
        val mergedData = if (sync.failed && oldRoot is Imported) merge(oldRoot.data, newData) else newData

        val newRoot = tryCreateImportedRoot(sync.workingDir, LastModifiedFiles()) { mergedData } ?: return null
        val buildRootDir = newRoot.dir ?: return null

        GradleBuildRootDataSerializer.write(buildRootDir, mergedData)
        newRoot.saveLastModifiedFiles()

        return newRoot
    }

    private fun merge(old: GradleBuildRootData, new: GradleBuildRootData): GradleBuildRootData {
        val roots = old.projectRoots.toMutableSet()
        roots.addAll(new.projectRoots)

        val models = old.models.associateByTo(mutableMapOf()) { it.file }
        new.models.associateByTo(models) { it.file }

        return GradleBuildRootData(new.importTs, roots, new.gradleHome, new.javaHome, models.values)
    }

    private val modifiedFilesCheckScheduled = AtomicBoolean()
    private val modifiedFiles = ConcurrentLinkedQueue<String>()

    fun scheduleModifiedFilesCheck(filePath: String) {
        modifiedFiles.add(filePath)
        if (modifiedFilesCheckScheduled.compareAndSet(false, true)) {
            BackgroundTaskUtil.executeOnPooledThread(project) {
                if (modifiedFilesCheckScheduled.compareAndSet(true, false)) {
                    checkModifiedFiles()
                }
            }
        }
    }

    private fun checkModifiedFiles() {
        updateNotifications(restartAnalyzer = false) { true }

        roots.list.forEach {
            it.saveLastModifiedFiles()
        }

        // process modifiedFiles queue
        while (true) {
            val file = modifiedFiles.poll() ?: break

            // detect gradle version change
            val buildDir = findGradleWrapperPropertiesBuildDir(file)
            if (buildDir != null) {
                actualizeBuildRoot(buildDir, null)
            }
        }
    }

    fun updateStandaloneScripts(update: StandaloneScriptsUpdater.() -> Unit) {
        val changes = StandaloneScriptsUpdater.collectChanges(delegate = roots, update)

        updateNotifications { it in changes.new || it in changes.removed }
        loadStandaloneScriptConfigurations(changes.new)
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
                val version = GradleInstallationManager.getGradleVersion(newPath)
                reloadBuildRoot(linkedProjectPath, version)
            }

            override fun onGradleDistributionTypeChange(currentValue: DistributionType?, linkedProjectPath: String) {
                reloadBuildRoot(linkedProjectPath, null)
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
    private fun actualizeBuildRoot(workingDir: String, gradleVersion: String?): GradleBuildRoot? {
        val actualSettings = getGradleProjectSettings(workingDir)
        val buildRoot = getBuildRootByWorkingDir(workingDir)

        val version = gradleVersion ?: actualSettings?.let { getGradleVersion(project, it) }
        return when {
            buildRoot != null -> {
                when {
                    !buildRoot.checkActual(version) -> reloadBuildRoot(workingDir, version)
                    else -> buildRoot
                }
            }
            actualSettings != null && version != null -> {
                loadLinkedRoot(actualSettings, version)
            }
            else -> null
        }
    }

    private fun GradleBuildRoot.checkActual(version: String?): Boolean {
        if (version == null) return false

        val knownAsSupported = this !is Legacy
        val shouldBeSupported = kotlinDslScriptsModelImportSupported(version)
        return knownAsSupported == shouldBeSupported
    }

    private fun reloadBuildRoot(rootPath: String, version: String?): GradleBuildRoot? {
        val settings = getGradleProjectSettings(rootPath)
        if (settings == null) {
            remove(rootPath)
            return null
        } else {
            val gradleVersion = version ?: getGradleVersion(project, settings)
            val newRoot = loadLinkedRoot(settings, gradleVersion)
            add(newRoot)
            return newRoot
        }
    }

    private fun loadLinkedRoot(settings: GradleProjectSettings, version: String = getGradleVersion(project, settings)): GradleBuildRoot {
        if (!enabled) {
            return Legacy(settings)
        }

        val supported = kotlinDslScriptsModelImportSupported(version)

        return when {
            supported -> tryLoadFromFsCache(settings, version) ?: New(settings)
            else -> Legacy(settings)
        }
    }

    private fun tryLoadFromFsCache(settings: GradleProjectSettings, version: String): Imported? {
        return tryCreateImportedRoot(settings.externalProjectPath) {
            GradleBuildRootDataSerializer.read(it)?.let { data ->
                val gradleHome = data.gradleHome
                if (gradleHome.isNotBlank() && GradleInstallationManager.getGradleVersion(gradleHome) != version) return@let null

                addFromSettings(data, settings)
            }
        }
    }

    private fun addFromSettings(
        data: GradleBuildRootData,
        settings: GradleProjectSettings
    ) = data.copy(projectRoots = data.projectRoots.toSet() + settings.modules)

    private fun tryCreateImportedRoot(
        externalProjectPath: String,
        lastModifiedFiles: LastModifiedFiles = loadLastModifiedFiles(externalProjectPath) ?: LastModifiedFiles(),
        dataProvider: (buildRoot: VirtualFile) -> GradleBuildRootData?
    ): Imported? {
        try {
            val buildRoot = VfsUtil.findFile(Paths.get(externalProjectPath), true) ?: return null
            val data = dataProvider(buildRoot) ?: return null

            return Imported(externalProjectPath, data, lastModifiedFiles)
        } catch (e: Exception) {
            scriptingErrorLog("Cannot load script configurations from file attributes for $externalProjectPath", e)
            return null
        }
    }

    private fun add(newRoot: GradleBuildRoot) {
        val old = roots.add(newRoot)
        if (old is Imported && newRoot !is Imported) {
            removeData(old.pathPrefix)
        }
        if (old !is Legacy || newRoot !is Legacy) {
            updater.invalidateAndCommit()
        }

        updateNotifications { it.startsWith(newRoot.pathPrefix) }
    }

    private fun remove(rootPath: String) {
        val removed = roots.remove(rootPath)
        if (removed is Imported) {
            removeData(rootPath)
            updater.invalidateAndCommit()
        }

        updateNotifications { it.startsWith(rootPath) }
    }

    private fun removeData(rootPath: String) {
        val buildRoot = LocalFileSystem.getInstance().findFileByPath(rootPath)
        if (buildRoot != null) {
            GradleBuildRootDataSerializer.remove(buildRoot)
            LastModifiedFiles.remove(buildRoot)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun updateNotifications(
        restartAnalyzer: Boolean = true,
        shouldUpdatePath: (String) -> Boolean
    ) {
        if (!project.isOpen) return

        // import notification is a balloon, so should be shown only for selected editor
        FileEditorManager.getInstance(project).selectedEditor?.file?.let {
            if (shouldUpdatePath(it.path) && maybeAffectedGradleProjectFile(it.path)) {
                updateFloatingAction(it)
            }
        }

        val openedScripts = FileEditorManager.getInstance(project).selectedEditors
            .mapNotNull { it.file }
            .filter {
                shouldUpdatePath(it.path) && maybeAffectedGradleProjectFile(it.path)
            }

        if (openedScripts.isEmpty()) return

        GlobalScope.launch(EDT(project)) {
            if (project.isDisposed) return@launch

            openedScripts.forEach {
                if (isApplicable(it)) {
                    DefaultScriptingSupport.getInstance(project).ensureNotificationsRemoved(it)
                }

                if (restartAnalyzer) {
                    // this required only for "pause" state
                    val ktFile = PsiManager.getInstance(project).findFile(it)
                    if (ktFile != null) DaemonCodeAnalyzer.getInstance(project).restart(ktFile)
                }

                EditorNotifications.getInstance(project).updateAllNotifications()
            }
        }
    }

    private fun updateFloatingAction(file: VirtualFile) {
        if (isConfigurationOutOfDate(file)) {
            scriptConfigurationsNeedToBeUpdated(project, file)
        } else {
            scriptConfigurationsAreUpToDate(project)
        }
    }

    private fun loadStandaloneScriptConfigurations(files: MutableSet<String>) {
        runReadAction {
            files.forEach {
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(it)
                if (virtualFile != null) {
                    val ktFile = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile
                    if (ktFile != null) {
                        DefaultScriptingSupport.getInstance(project)
                            .ensureUpToDatedConfigurationSuggested(ktFile, skipNotification = true)
                    }
                }
            }
        }
    }

    companion object {
        fun getInstance(project: Project): GradleBuildRootsManager =
            EPN.getPoint(project).extensionList.firstIsInstance()
    }
}
