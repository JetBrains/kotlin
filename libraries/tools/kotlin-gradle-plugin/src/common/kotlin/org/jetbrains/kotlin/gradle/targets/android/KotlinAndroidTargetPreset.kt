/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPlugin.Companion.dynamicallyApplyWhenAndroidPluginIsApplied
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.AndroidGradlePluginIsMissing
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.utils.findAppliedAndroidPluginIdOrNull
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset

import javax.inject.Inject

internal abstract class KotlinAndroidTargetPreset @Inject constructor(
    private val project: Project
) : InternalKotlinTargetPreset<KotlinAndroidTarget> {

    override val name: String = PRESET_NAME

    override fun createTargetInternal(name: String): KotlinAndroidTarget {

        /*
        Android Gradle Plugin is required:
        Creating target will fail with Linkage Error instead

        Could not create an instance of type org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget.
            > Could not generate a decorated class for type KotlinAndroidTarget.
            > com/android/build/gradle/api/BaseVariant
         */
        val androidPluginId = project.findAppliedAndroidPluginIdOrNull()
        if (androidPluginId == null) {
            project.reportDiagnostic(AndroidGradlePluginIsMissing(Throwable()))
        } else {
            project.reportDiagnostic(KotlinToolingDiagnostics.NonKmpAgpIsDeprecated(androidPluginId))
        }

        return project.objects.KotlinAndroidTarget(project, name, true).apply {
            targetPreset = this@KotlinAndroidTargetPreset
            project.dynamicallyApplyWhenAndroidPluginIsApplied({ this })
        }
    }

    companion object {
        const val PRESET_NAME = "android"
    }
}
