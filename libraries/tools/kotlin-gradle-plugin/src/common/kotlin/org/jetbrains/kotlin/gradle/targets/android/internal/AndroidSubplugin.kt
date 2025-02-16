/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerProject
import javax.inject.Inject

// Use apply plugin: 'kotlin-android-extensions' to enable Android Extensions in an Android project.
// Not working anymore - keeping for upgrade path from old Kotlin versions.
class AndroidExtensionsSubpluginIndicator @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) :
    Plugin<Project> {
    override fun apply(project: Project) {
        project.reportDiagnosticOncePerProject(
            KotlinToolingDiagnostics.AndroidExtensionPluginRemoval()
        )
    }
}
