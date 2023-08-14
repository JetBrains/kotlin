package org.jetbrains.kotlin.gradle.fus

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class FusStatisticsPlugin @Inject constructor(
    private val providerFactory: ProviderFactory
) : Plugin<Project> {
    override fun apply(project: Project) {
        GradleBuildFusStatisticsService.registerIfAbsent(project).get()
    }
}