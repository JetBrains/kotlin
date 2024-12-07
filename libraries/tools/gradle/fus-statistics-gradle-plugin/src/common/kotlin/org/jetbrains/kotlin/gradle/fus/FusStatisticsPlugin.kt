package org.jetbrains.kotlin.gradle.fus

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.fus.internal.registerGradleBuildFusStatisticsServiceIfAbsent
import javax.inject.Inject

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * FusStatisticsPlugin is a Gradle plugin that registers a BuildService to collect and report
 * feature usage statistics during the build process.
 */
class FusStatisticsPlugin @Inject constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        val uidService = BuildUidService.registerIfAbsent(project)
        registerGradleBuildFusStatisticsServiceIfAbsent(project, uidService)
    }
}