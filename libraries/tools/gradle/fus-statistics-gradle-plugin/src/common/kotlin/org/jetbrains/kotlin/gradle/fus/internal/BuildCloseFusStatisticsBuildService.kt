/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.logging.Logging

internal abstract class BuildCloseFusStatisticsBuildService : InternalGradleBuildFusStatisticsService() {

    private val log = Logging.getLogger(this.javaClass)

    init {
        log.debug("InternalGradleBuildFusStatisticsService is initialized for $buildId build")
    }

    //since Gradle
    override fun close() {
        log.debug("InternalGradleBuildFusStatisticsService is closed for $buildId build")

        //since Gradle 8.1 flow action [BuildFinishFlowAction] is used to collect all metrics and write them down in a single file
        writeDownFusMetrics(log)
    }
}