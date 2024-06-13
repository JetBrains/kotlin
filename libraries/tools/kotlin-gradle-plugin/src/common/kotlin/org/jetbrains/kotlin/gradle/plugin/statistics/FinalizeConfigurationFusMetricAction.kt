/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.await


internal val FinalizeConfigurationFusMetricAction = KotlinProjectSetupCoroutine {
    KotlinPluginLifecycle.Stage.ReadyForExecution.await()

    val projectConfigurationService =
        project.gradle.sharedServices.registrations.findByName(ProjectConfigurationFusService.getServiceName(project))?.also {
            val parameters = it.parameters as ProjectConfigurationFusService.Parameters
            parameters.configurationMetrics.finalizeValue()
        }

    project.gradle.sharedServices.registrations.findByName(BuildFusService.serviceName)?.also {
        val parameters = it.parameters as BuildFusService.Parameters
        parameters.generalConfigurationMetrics.finalizeValue()
        projectConfigurationService?.service?.orNull?.also {
            parameters.configurationMetrics.add((it.parameters as ProjectConfigurationFusService.Parameters).configurationMetrics)
        }
    }
}