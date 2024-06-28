/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

/**
 * Dummy service is used to avoid data collection without user's consent
 */
internal abstract class DummyGradleBuildFusStatisticsService : GradleBuildFusStatisticsBuildService() {
    override fun reportMetric(name: String, value: Boolean, subprojectName: String?) {}

    override fun reportMetric(name: String, value: Number, subprojectName: String?) {}

    override fun reportMetric(name: String, value: String, subprojectName: String?) {}

}