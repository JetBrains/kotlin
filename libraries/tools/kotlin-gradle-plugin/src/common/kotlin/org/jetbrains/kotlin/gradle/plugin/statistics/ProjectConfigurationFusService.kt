/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class ProjectConfigurationFusService : BuildService<ProjectConfigurationFusService.Parameters>, AutoCloseable {
    interface Parameters : BuildServiceParameters {
        val configurationMetrics: Property<MetricContainer>
    }

    companion object {
        fun getServiceName(project: Project): String =
            "${project.path}_${ProjectConfigurationFusService::class.simpleName}_${ProjectConfigurationFusService::class.java.classLoader.hashCode()}"

        //register a new service for every project to collect configuration metrics
        fun registerIfAbsent(project: Project): Provider<ProjectConfigurationFusService> {
            return project.gradle.sharedServices.registerIfAbsent(
                getServiceName(project),
                ProjectConfigurationFusService::class.java
            ) { spec ->
                spec.parameters.configurationMetrics.set(project.provider {
                    KotlinProjectConfigurationMetrics.collectMetrics(project)
                })
            }
        }
    }

    override fun close() {
    }
}