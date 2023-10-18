/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.runDeprecationDiagnostics
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.target.presetName

class KotlinMultiplatformPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleCompatibility("the Kotlin Multiplatform plugin")
        runDeprecationDiagnostics(project)

        project.plugins.apply(JavaBasePlugin::class.java)
    }

    companion object {
        const val METADATA_TARGET_NAME = "metadata"

        internal fun sourceSetFreeCompilerArgsPropertyName(sourceSetName: String) =
            "kotlin.mpp.freeCompilerArgsForSourceSet.$sourceSetName"
    }
}
