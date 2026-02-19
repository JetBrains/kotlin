/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.fus.GradleBuildFusStatisticsService
import org.jetbrains.kotlin.gradle.fus.UniqueId

/**
 * NoConsent build service is used to avoid data collection without user's consent
 */
internal abstract class NoConsentGradleBuildFusService : GradleBuildFusStatisticsService<BuildServiceParameters.None> {
    override fun reportMetric(name: String, value: Boolean, uniqueId: UniqueId) {}

    override fun reportMetric(name: String, value: Number, uniqueId: UniqueId) {}

    override fun reportMetric(name: String, value: String, uniqueId: UniqueId) {}

    override fun close() {}

}