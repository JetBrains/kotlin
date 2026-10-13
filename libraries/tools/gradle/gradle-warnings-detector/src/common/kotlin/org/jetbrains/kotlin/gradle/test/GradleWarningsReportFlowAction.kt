/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test

import org.gradle.api.GradleException
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input

internal abstract class GradleWarningsReportFlowAction : FlowAction<GradleWarningsReportFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:ServiceReference
        val warningsReporter: Property<GradleWarningsReporter>

        @get:Input
        val configurationTimeWarnings: SetProperty<String>

        @get:Input
        val configurationTimeIgnoredReasons: SetProperty<String>
    }

    override fun execute(parameters: Parameters) {
        val warningsReporter = parameters.warningsReporter.get()
        val ignoredReasons = parameters.configurationTimeIgnoredReasons.get() + warningsReporter.ignoredReasons
        val warnings = parameters.configurationTimeWarnings.get() + warningsReporter.warnings

        if (ignoredReasons.isNotEmpty()) {
            logger.info(
                "[$marker] Ignored deprecation warnings caused by third-party plugins:\n${ignoredReasons.joinToString("\n")}"
            )
        }
        if (warnings.isNotEmpty()) {
            logger.warn("[$marker] Deprecation warnings were found during this build:\n${warnings.joinToString("\n")}")
            if (warningsReporter.failOnWarnings) {
                throw GradleException("Unexpected warnings were detected in the build!")
            }
        }
    }

    private companion object {
        private val logger = Logging.getLogger(GradleWarningsReportFlowAction::class.java)
        private val marker = GradleWarningsDetectorPlugin::class.java.simpleName
    }
}
