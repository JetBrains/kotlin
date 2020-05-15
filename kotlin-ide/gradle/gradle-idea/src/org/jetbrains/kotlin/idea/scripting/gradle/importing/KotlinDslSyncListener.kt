/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.scripting.gradle.GradleScriptDefinitionsContributor

class KotlinDslSyncListener : ExternalSystemTaskNotificationListenerAdapter() {
    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
        if (!isGradleProjectImport(id)) return
        if (workingDir == null) return

        gradleState.isSyncInProgress = true

        val project = id.findProject() ?: return

        project.kotlinGradleDslSync[id] = KotlinDslGradleBuildSync(workingDir, id)
    }

    override fun onEnd(id: ExternalSystemTaskId) {
        if (!isGradleProjectImport(id)) return

        gradleState.isSyncInProgress = false

        val project = id.findProject() ?: return

        @Suppress("DEPRECATION")
        ScriptDefinitionContributor.find<GradleScriptDefinitionsContributor>(project)?.reloadIfNecessary()

        val sync = project.kotlinGradleDslSync.remove(id)
        if (sync != null) {
            // For Gradle 6.0 or higher
            saveScriptModels(project, sync)
        }
    }

    override fun onCancel(id: ExternalSystemTaskId) {
        if (!isGradleProjectImport(id)) return

        gradleState.isSyncInProgress = false

        val project = id.findProject() ?: return

        project.kotlinGradleDslSync.remove(id)
    }

    private fun isGradleProjectImport(id: ExternalSystemTaskId): Boolean {
        return id.type == ExternalSystemTaskType.RESOLVE_PROJECT || id.projectSystemId == GRADLE_SYSTEM_ID
    }

    companion object {
        internal val gradleState = GradleSyncState()
    }
}

// TODO: state should be stored by gradle build,
// now it is marked as complete after first gradle project was imported
internal class GradleSyncState {
    var isSyncInProgress: Boolean = false
}
