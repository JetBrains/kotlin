/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.test

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.operations.BuildOperationListenerManager
import javax.inject.Inject

class GradleWarningsDetectorPlugin @Inject constructor(
    private val buildOperationListenerManager: BuildOperationListenerManager
) : Plugin<Settings> {
    private val logger: Logger = Logging.getLogger(this.javaClass)

    override fun apply(target: Settings) {
        logger.warn("[${GradleWarningsDetectorPlugin::class.java.simpleName}] The plugin is being applied")
        val warningsReporter = GradleWarningsReporter.registerIfAbsent(target.gradle)
        val deprecationBuildOperationListener = DeprecationBuildOperationListener(warningsReporter)
        buildOperationListenerManager.addListener(deprecationBuildOperationListener)
        warningsReporter.get().executeAtBuildFinish = {
            buildOperationListenerManager.removeListener(deprecationBuildOperationListener)
        }
    }
}