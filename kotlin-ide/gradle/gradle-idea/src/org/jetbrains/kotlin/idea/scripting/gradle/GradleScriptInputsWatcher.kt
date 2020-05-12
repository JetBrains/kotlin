/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.*
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

@State(
    name = "KotlinBuildScriptsModificationInfo",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class GradleScriptInputsWatcher(val project: Project) : PersistentStateComponent<GradleScriptInputsWatcher.Storage> {
    private var storage = Storage()

    private var cachedGradleProjectsRoots: Set<String>? = null

    fun getGradleProjectsRoots(): Set<String> {
        if (cachedGradleProjectsRoots == null) {
            cachedGradleProjectsRoots = computeGradleProjectRoots(project)
        }
        return cachedGradleProjectsRoots ?: emptySet()
    }

    fun saveGradleProjectRootsAfterImport(roots: Set<String>) {
        val oldRoots = cachedGradleProjectsRoots
        if (oldRoots != null && oldRoots.isNotEmpty()) {
            cachedGradleProjectsRoots = oldRoots + roots
        } else {
            cachedGradleProjectsRoots = roots
        }
    }

    private fun computeGradleProjectRoots(project: Project): Set<String> {
        val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        if (gradleSettings.getLinkedProjectsSettings().isEmpty()) return setOf()

        val projectSettings = gradleSettings.getLinkedProjectsSettings().filterIsInstance<GradleProjectSettings>().firstOrNull()
            ?: return setOf()

        return projectSettings.modules.takeIf { it.isNotEmpty() } ?: setOf(projectSettings.externalProjectPath)
    }

    fun startWatching() {
        addVfsListener(this)
    }

    fun areRelatedFilesUpToDate(file: VirtualFile, timeStamp: Long): Boolean {
        return storage.lastModifiedTimeStampExcept(file.path) < timeStamp
    }

    class Storage {
        private val lastModifiedFiles = LastModifiedFiles()

        fun lastModifiedTimeStampExcept(filePath: String): Long {
            return lastModifiedFiles.lastModifiedTimeStampExcept(filePath)
        }

        fun fileChanged(filePath: String, ts: Long) {
            lastModifiedFiles.fileChanged(ts, filePath)
        }
    }

    override fun getState(): Storage {
        return storage
    }

    override fun loadState(state: Storage) {
        this.storage = state
    }

    fun fileChanged(filePath: String, ts: Long) {
        storage.fileChanged(filePath, ts)
    }

    fun clearState() {
        storage = Storage()
    }

    @TestOnly
    fun clearAndRefillState() {
        loadState(project.service<GradleScriptInputsWatcher>().state)
    }
}