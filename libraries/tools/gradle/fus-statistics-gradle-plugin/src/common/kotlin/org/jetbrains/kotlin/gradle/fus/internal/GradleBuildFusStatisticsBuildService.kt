/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.UsesGradleBuildFusStatisticsService

private val serviceClass = GradleBuildFusStatisticsService::class.java
internal val serviceName = "${serviceClass.name}_${serviceClass.classLoader.hashCode()}"
internal val log = Logging.getLogger(GradleBuildFusStatisticsService::class.java)

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
    val customPath: String = project.getFusDirectory()

    val objectFactory = project.objects
    return if (customPath.isBlank() && isCiBuild()) {
        val ciProperty = detectedCiProperty()
        log.debug("Fus metrics won't be collected for CI build. (CI build detected via environment variable $ciProperty)")
        project.gradle.sharedServices.registerIfAbsent(serviceName, NoConsentGradleBuildFusService::class.java) {}
    } else {
        val fusService = project.gradle.sharedServices.registerIfAbsent(serviceName, BuildFlowFusStatisticsBuildService::class.java) {
            it.parameters.fusStatisticsRootDirPath.value(customPath).disallowChanges()
            it.parameters.buildId.value(uidService.map { it.buildId }).disallowChanges()
        }
        FusBuildFinishFlowManager.getInstance(project).subscribeForBuildFinish(fusService)
        // Ensure GradleBuildFusStatisticsService is created at the same time as BuildFinishBuildService to ensure the same buildId is used
        objectFactory.newInstance(BuildEventsListenerRegistryHolder::class.java).listenerRegistry.onTaskCompletion(fusService)
        fusService
    }
}
