/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.plugin.launch

internal val KotlinToolingDiagnosticsSetupAction = KotlinProjectSetupAction {
    val collectorProvider = kotlinToolingDiagnosticsCollectorProvider
    val diagnosticRenderingOptions = ToolingDiagnosticRenderingOptions.forProject(this)

    // Setup reporting from tasks
    tasks.withType(UsesKotlinToolingDiagnostics::class.java).configureEach {
        it.usesService(collectorProvider)
        it.toolingDiagnosticsCollector.value(collectorProvider)
        it.diagnosticRenderingOptions.set(diagnosticRenderingOptions)
    }

    // Launch checkers. Note that they are invoked eagerly to give them a fine-grained
    // control over the lifecycle
    launchKotlinGradleProjectCheckers()

    // Setup a task that will abort the build if errors will be reported. This task should be the first in the taskgraph
    project.locateOrRegisterCheckKotlinGradlePluginErrorsTask()

    // Schedule diagnostics rendering
    launch {
        configurationResult.await()
        renderReportedDiagnostics(
            collectorProvider.get().getDiagnosticsForProject(project),
            logger,
            diagnosticRenderingOptions
        )
    }

    // Schedule switching of Collector to transparent mode, so that any diagnostics reported
    // after projects are evaluated will be transparently rendered right away instead of being
    // silently swallowed
    gradle.projectsEvaluated {
        collectorProvider.get().switchToTransparentMode()
    }
}
