/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.project.model.*

/**
 * Gradle plugin that provides [KpmCompilerPlugin] from applied [Project]
 */
interface GradleKpmCompilerPlugin : Plugin<Project> {
    val kpmCompilerPlugin: KpmCompilerPlugin
}
