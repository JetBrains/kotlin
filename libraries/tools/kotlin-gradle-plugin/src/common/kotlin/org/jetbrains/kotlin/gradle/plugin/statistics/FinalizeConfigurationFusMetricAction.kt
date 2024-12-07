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
        val parameters = it.parameters as BuildFusService.Parameters
        parameters.generalConfigurationMetrics.finalizeValue()
        parameters.configurationMetrics.add(KotlinProjectConfigurationMetrics.collectMetrics(project))
    }
}