/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.Project
import org.gradle.api.flow.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
import org.gradle.tooling.Failure
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.util.GradleVersion
import org.gradle.internal.extensions.core.serviceOf
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.buildEventsListenerRegistry
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.gradleVersion
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.concurrent.atomic.AtomicBoolean

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
    if (!useXcodeMessageStyle.get()) {
        return@action
    }

    val xcodeBuildErrorService = gradle.registerClassLoaderScopedBuildService(XcodeBuildErrorService::class)
    project.buildEventsListenerRegistry.onTaskCompletion(xcodeBuildErrorService)

    if (gradleVersion >= GradleVersion.version("8.1")) {
        val flowProviders = serviceOf<FlowProviders>()
        serviceOf<FlowScope>().always(
            XcodeBuildConfigurationCacheErrorsReporter::class.java,
            {
                it.parameters.buildResult.set(flowProviders.buildWorkResult)
                it.parameters.xcodeBuildErrorService.set(xcodeBuildErrorService)
            }
        )
    }
}

internal abstract class XcodeBuildErrorService : BuildService<BuildServiceParameters.None>, OperationCompletionListener {
    val receivedFinishEventWithFailure = AtomicBoolean(false)

    override fun onFinish(event: FinishEvent?) {
        val result = event?.result
        if (result != null && result is TaskFailureResult) {
            receivedFinishEventWithFailure.set(true)
            result.failures.forEach { failure ->
                failure.print(0)
            }
        }
    }

    private fun Failure.print(depth: Int) {
        val message = message ?: toString()
        val padding = " ".repeat(depth * 2)
        message.lineSequence().forEach { line ->
            System.err.println("error: ${padding}${line}")
        }
        causes.forEach {
            it.print(depth + 1)
        }
    }
}

internal abstract class XcodeBuildConfigurationCacheErrorsReporter : FlowAction<XcodeBuildConfigurationCacheErrorsReporter.Params> {

    private fun Throwable.printFailure(depth: Int) {
        val message = message ?: toString()
        val padding = " ".repeat(depth * 2)
        message.lineSequence().forEach { line ->
            System.err.println("error: ${padding}${line}")
        }
        cause?.printFailure(depth + 1)
    }

    interface Params : FlowParameters {
        @get:Input
        val buildResult: Property<BuildWorkResult>

        @get:Input
        val xcodeBuildErrorService: Property<XcodeBuildErrorService>
    }

    override fun execute(parameters: Params) {
        /**
         * In case of a configuration serialization error the build service is not going to receive the finish event, and we print it with
         * the FlowAction. In case of a build failure we want to print with the build service because FlowAction is only supported since 8.1
         */
        if (!parameters.xcodeBuildErrorService.get().receivedFinishEventWithFailure.compareAndSet(false, true)) return
        val failure = parameters.buildResult.get().failure
        if (failure.isPresent) {
            // Don't print the root failure because it's duplicated with its cause
            failure.get().cause?.printFailure(0)
        }
    }
}