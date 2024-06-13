/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheEnabled

internal val Project.useXcodeMessageStyle: Provider<Boolean>
    get() = nativeProperties
        .isUseXcodeMessageStyleEnabled
        .orElse(isXcodeTasksRequested)

private val Project.isXcodeTasksRequested: Provider<Boolean>
    get() = providers.provider {
        gradle.startParameter.taskNames.any { requestedTask ->
            val name = requestedTask.substringAfterLast(':')
            val isSyncTask = name == KotlinCocoapodsPlugin.SYNC_TASK_NAME
            val isEmbedAndSignTask = name.startsWith(AppleXcodeTasks.embedAndSignTaskPrefix) && name.endsWith(AppleXcodeTasks.embedAndSignTaskPostfix)
            isSyncTask || isEmbedAndSignTask
        }
    }

internal val AddBuildListenerForXCodeSetupAction = KotlinProjectSetupAction action@{
    if (isConfigurationCacheEnabled) {
        // TODO https://youtrack.jetbrains.com/issue/KT-55832
        // Configuration cache case will be supported later
        return@action
    }

    if (!useXcodeMessageStyle.get()) {
        return@action
    }

    gradle.addBuildListener(XcodeBuildErrorListener)
}

private object XcodeBuildErrorListener : BuildAdapter() {
    @Suppress("OVERRIDE_DEPRECATION") // Listener is added only when configuration cache is disabled
    override fun buildFinished(result: BuildResult) {
        if (result.failure != null) {
            val rootCause = generateSequence(result.failure) { it.cause }.last()
            val message = rootCause.message ?: rootCause.toString()
            System.err.println("error: ${message.lineSequence().first()}")
        }
    }
}