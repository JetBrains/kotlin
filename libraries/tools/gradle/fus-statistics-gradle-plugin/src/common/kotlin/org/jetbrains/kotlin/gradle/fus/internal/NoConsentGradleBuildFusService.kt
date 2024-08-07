/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * NoConsent build service is used to avoid data collection without user's consent
 */
internal abstract class NoConsentGradleBuildFusService : GradleBuildFusStatisticsBuildService(), BuildService<BuildServiceParameters.None> {
    override fun reportMetric(name: String, value: Boolean, subprojectName: String?) {}

    override fun reportMetric(name: String, value: Number, subprojectName: String?) {}

    override fun reportMetric(name: String, value: String, subprojectName: String?) {}

}