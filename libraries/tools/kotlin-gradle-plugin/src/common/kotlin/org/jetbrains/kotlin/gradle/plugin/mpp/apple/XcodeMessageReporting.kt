/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import com.intellij.util.containers.orNull
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheEnabled
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
        dataflowApiSupported() -> project.objects.newInstance<XcodeBuildFlowManager>().subscribeForBuildResult()
        !isConfigurationCacheEnabled -> gradle.addBuildListener(XcodeBuildErrorListener)
    }
}

private fun dataflowApiSupported() = GradleVersion.current().baseVersion >= GradleVersion.version("8.1")

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
        reportBuildError(parameters.failure.get().orNull())
    }
}

private object XcodeBuildErrorListener : BuildAdapter() {
    @Suppress("OVERRIDE_DEPRECATION") // Listener is added only for old Gradle versions
    override fun buildFinished(result: BuildResult) {
        reportBuildError(result.failure)
    }
}

private fun reportBuildError(failure: Throwable?) {
    if (failure != null) {
        val rootCause = generateSequence(failure) { it.cause }.last()
        val message = rootCause.message ?: rootCause.toString()
        System.err.println("error: ${message.lineSequence().first()}")
    }
}