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

    project.gradle.sharedServices.registrations.findByName(BuildFusService.serviceName)?.also {
        val parameters = it.parameters
        if (parameters is ConfigurationMetricsBuildFusParameters) {
            //build service parameter is used,
            //it is important to avoid service parameters initialization before all configuration metrics are collected
            parameters.generalConfigurationMetrics.finalizeValue()
            parameters.configurationMetrics.add(KotlinProjectConfigurationMetrics.collectMetrics(project))
        } else {
            (parameters as BuildFusService.Parameters).generalConfigurationMetrics.finalizeValue()

            //build service field is used,
            //it is safe to access build service, as configuration metrics will be cached in [BuildFinishFlowAction]
            val buildFusService = it.service.orNull as FlowActionBuildFusService
            buildFusService.addConfigurationTimeMetric(KotlinProjectConfigurationMetrics.collectMetrics(project))
        }
    }
}