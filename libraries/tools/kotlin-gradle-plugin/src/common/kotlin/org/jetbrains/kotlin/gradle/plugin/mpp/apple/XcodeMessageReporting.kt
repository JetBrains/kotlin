/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple


import org.gradle.api.Project
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.util.Optional
import javax.inject.Inject

internal val Project.useXcodeMessageStyle: Provider<Boolean>
    get() = nativeProperties
        .isUseXcodeMessageStyleEnabled
        .orElse(isXcodeTasksRequested)

private val Project.isXcodeTasksRequested: Provider<Boolean>
    get() = providers.provider {
        gradle.startParameter.taskNames.any { requestedTask ->
            val name = requestedTask.substringAfterLast(':')
            val isSyncTask = name == KotlinCocoapodsPlugin.SYNC_TASK_NAME
            val isEmbedAndSignTask = name.startsWith(AppleXcodeTasks.embedAndSignTaskPrefix) &&
                    name.endsWith(AppleXcodeTasks.embedAndSignTaskPostfix)
            isSyncTask || isEmbedAndSignTask
        }
    }

internal val AddBuildListenerForXcodeSetupAction = KotlinProjectSetupAction action@{
    when {
        !useXcodeMessageStyle.get() -> {}
        else -> project.objects.newInstance<XcodeBuildFlowManager>().subscribeForBuildResult()
    }
}

internal abstract class XcodeBuildFlowManager @Inject constructor(
    private val flowScope: FlowScope,
    private val flowProviders: FlowProviders,
) {

    fun subscribeForBuildResult() {
        flowScope.always(
            XcodeBuildFinishedAction::class.java
        ) { spec ->
            spec.parameters.failure.set(flowProviders.buildWorkResult.map { it.failure })
        }
    }
}

internal class XcodeBuildFinishedAction : FlowAction<XcodeBuildFinishedAction.Parameters> {

    interface Parameters : FlowParameters {
        @get:Input
        val failure: Property<Optional<Throwable>>
    }

    override fun execute(parameters: Parameters) {
        reportBuildError(parameters.failure.get().orElse(null))
    }
}

private fun reportBuildError(failure: Throwable?) {
    if (failure != null) {
        val rootCause = generateSequence(failure) { it.cause }.last()
        val message = rootCause.message ?: rootCause.toString()
        System.err.println("error: ${message.lineSequence().first()}")
    }
}
