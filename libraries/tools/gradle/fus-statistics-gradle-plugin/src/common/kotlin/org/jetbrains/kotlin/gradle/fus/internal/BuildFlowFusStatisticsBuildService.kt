/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

abstract class BuildFlowFusStatisticsBuildService : InternalGradleBuildFusStatisticsService() {
    override fun close() {
        // since Gradle 8.1 flow action [BuildFinishFlowAction] is used to collect all metrics and write them down in a single file
    }
}