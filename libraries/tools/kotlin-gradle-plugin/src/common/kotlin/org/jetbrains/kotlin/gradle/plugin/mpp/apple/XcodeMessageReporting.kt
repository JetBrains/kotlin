/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.BuildAdapter
import org.gradle.BuildResult
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
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.buildEventsListenerRegistry
import org.jetbrains.kotlin.gradle.plugin.flowProviders
import org.jetbrains.kotlin.gradle.plugin.flowScope
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheEnabled
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

    if (project.kotlinPropertiesProvider.xcodeErrorPrintingWithConfigurationCache) {
        val xcodeBuildErrorService = gradle.registerClassLoaderScopedBuildService(XcodeBuildErrorPrintingService::class)
        buildEventsListenerRegistry.onTaskCompletion(xcodeBuildErrorService)

        // Flow providers are only available since 8.1
        if (gradleVersion >= GradleVersion.version("8.1")) {
            val flowProviders = flowProviders
            flowScope.always(
                XcodeBuildConfigurationCacheErrorsReporter::class.java
            ) {
                it.parameters.buildResult.set(flowProviders.buildWorkResult)
                it.parameters.xcodeBuildErrorService.set(xcodeBuildErrorService)
            }
        }
    } else if (!isConfigurationCacheEnabled) {
        gradle.addBuildListener(XcodeBuildErrorListener)
    }
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

internal abstract class XcodeBuildErrorPrintingService : BuildService<BuildServiceParameters.None>, OperationCompletionListener {
    val receivedFinishEventWithFailure = AtomicBoolean(false)

    override fun onFinish(event: FinishEvent?) {
        val result = event?.result
        if (result != null && result is TaskFailureResult) {
            receivedFinishEventWithFailure.set(true)
            result.failures.printFailureForXcode()
        }
    }

    companion object {
        const val OUTPUT_LIMIT = 50
    }
}

internal abstract class XcodeBuildConfigurationCacheErrorsReporter : FlowAction<XcodeBuildConfigurationCacheErrorsReporter.Params> {

    interface Params : FlowParameters {
        @get:Input
        val buildResult: Property<BuildWorkResult>

        @get:Input
        val xcodeBuildErrorService: Property<XcodeBuildErrorPrintingService>
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
            listOfNotNull(failure.get().cause).printThrowableForXcode()
        }
    }
}

internal fun List<Throwable>.printThrowableForXcode() = printErrorForXcode(
    causes = { listOfNotNull(it.cause) },
    message = { it.message ?: it.toString() },
    lineLimit = XcodeBuildErrorPrintingService.OUTPUT_LIMIT,
)
internal fun List<Failure>.printFailureForXcode() = printErrorForXcode(
    causes = { it.causes },
    message = { it.message ?: it.toString() },
    lineLimit = XcodeBuildErrorPrintingService.OUTPUT_LIMIT,
)
internal fun <Error> List<Error>.printErrorForXcode(
    causes: (Error) -> List<Error>,
    message: (Error) -> String,
    printLine: (String) -> Unit = { System.err.println("error: ${it}") },
    lineLimit: Int,
    lineLimitMessage: String = "See full error in the build log"
) {
    var linesEmitted = 0
    val stack = map { Pair(it, 0) }.reversed().toMutableList()

    while (stack.isNotEmpty()) {
        val (error, depth) = stack.pop()
        val messageString = message(error)
        val padding = " ".repeat(depth * 2)
        messageString.lineSequence().forEach { line ->
            if (linesEmitted == lineLimit - 1) {
                printLine(lineLimitMessage)
                return
            }
            printLine("${padding}${line}")
            linesEmitted += 1
        }
        stack.addAll(
            causes(error).reversed().map {
                Pair(it, depth + 1)
            }
        )
    }
}