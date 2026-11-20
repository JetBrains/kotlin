/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test

import org.gradle.api.Plugin
import org.gradle.api.flow.FlowScope
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ProviderFactory
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal
import javax.inject.Inject

class GradleWarningsDetectorPlugin @Inject constructor(
    private val buildEventsListenerRegistry: BuildEventsListenerRegistry,
    private val flowScope: FlowScope,
    private val providers: ProviderFactory,
) : Plugin<Settings> {
    private val logger: Logger = Logging.getLogger(this.javaClass)

    override fun apply(target: Settings) {
        logger.warn("[${GradleWarningsDetectorPlugin::class.java.simpleName}] The plugin is being applied")
        val warningsReporter = GradleWarningsReporter.registerIfAbsent(target.gradle)
        // Keeps the reporter alive during the execution phase
        (buildEventsListenerRegistry as BuildEventListenerRegistryInternal).onOperationCompletion(warningsReporter)

        // The reporter is shared by all builds in the build tree, so only the root build reports its warnings
        if (target.gradle.parent != null) return
        flowScope.always(GradleWarningsReportFlowAction::class.java) { spec ->
            spec.parameters.warningsReporter.set(warningsReporter)
            spec.parameters.configurationTimeWarnings.set(providers.provider { warningsReporter.get().warnings })
            spec.parameters.configurationTimeIgnoredReasons.set(providers.provider { warningsReporter.get().ignoredReasons })
        }
    }
}
