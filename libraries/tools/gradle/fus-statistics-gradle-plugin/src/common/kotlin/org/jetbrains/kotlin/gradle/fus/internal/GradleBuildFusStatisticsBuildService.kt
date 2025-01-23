/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.internal.RegisteredBuildServiceProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService

private const val statisticsIsEnabled: Boolean = true //KT-59629 Wait for user confirmation before start to collect metrics
private const val FUS_STATISTICS_PATH = "kotlin.session.logger.root.path"
private val serviceClass = GradleBuildFusStatisticsService::class.java
internal val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"
private val log = Logging.getLogger(GradleBuildFusStatisticsService::class.java)

fun registerGradleBuildFusStatisticsServiceIfAbsent(
    project: Project,
    uidService: Provider<BuildUidService>,
): Provider<out GradleBuildFusStatisticsService<out BuildServiceParameters>> {
    return registerIfAbsent(project, uidService).also { service ->
        project.tasks.withType(UsesGradleBuildFusStatisticsService::class.java).configureEach { task ->
            task.fusStatisticsBuildService.value(service).disallowChanges()
            task.usesService(service)
        }
    }
}

private fun registerIfAbsent(
    project: Project,
    uidService: Provider<BuildUidService>,
): Provider<out GradleBuildFusStatisticsService<out BuildServiceParameters>> {
    project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
        @Suppress("UNCHECKED_CAST")
        return it.service as Provider<GradleBuildFusStatisticsService<out BuildServiceParameters>>
    }
    val customPath: String =
        project.providers.gradleProperty(FUS_STATISTICS_PATH).orNull ?: project.gradle.gradleUserHomeDir.path


    return if (!statisticsIsEnabled || customPath.isBlank()) {
        log.info(
            "Fus metrics wont be collected as statistic was " +
                    (if (statisticsIsEnabled) "enabled" else "disabled") +
                    if (customPath.isBlank()) " and custom path is blank" else ""
        )
        project.gradle.sharedServices.registerIfAbsent(serviceName, NoConsentGradleBuildFusService::class.java) {}
    } else if (GradleVersion.current().baseVersion < GradleVersion.version("8.1")) {
        val fusService = project.gradle.sharedServices.registerIfAbsent(serviceName, BuildCloseFusStatisticsBuildService::class.java) {
            it.parameters.fusStatisticsRootDirPath.value(customPath).disallowChanges()
            it.parameters.configurationMetrics.empty()
            it.parameters.buildId.value(uidService.map { it.buildId }).disallowChanges()
        }
        project.registerTaskCreatingFusService(fusService)
        fusService
    } else {
        val fusService = project.gradle.sharedServices.registerIfAbsent(serviceName, BuildFlowFusStatisticsBuildService::class.java) {
            it.parameters.fusStatisticsRootDirPath.value(customPath).disallowChanges()
        }
        FusBuildFinishFlowManager.getInstance(project).subscribeForBuildFinish(fusService, uidService.map { it.buildId })
        fusService
    }
}

private fun Project.registerTaskCreatingFusService(
    fusService: Provider<BuildCloseFusStatisticsBuildService>,
) {
    //This task is necessary to delay FUS service creation onto execution time on Gradle<8.1 where BuildFlowFinishAction could not be used.
    //Service needs to be created, so Gradle will close it at the end of the build to write fus metrics.
    //Service could not be created on configuration time because then all service parameters will be finalized
    // and newly added metrics will be not accounted for.
    val writeFusTask = tasks.register("kotlinFus", DefaultTask::class.java) { task ->
        task.doNotTrackState("Task is stateless")

        task.usesService(fusService)
        task.doLast {
            // Triggering FUS service creation, so when only configuration metrics are added - they are still written
            fusService.get()
        }
    }

    val startTaskName = project.gradle.startParameter.taskNames.first()
    project.tasks.configureEach { t ->
        if (t.name == startTaskName) {
            t.finalizedBy(writeFusTask)
        }
    }
}
