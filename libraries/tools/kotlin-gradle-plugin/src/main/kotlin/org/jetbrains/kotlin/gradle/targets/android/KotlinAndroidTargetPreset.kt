/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPlugin.Companion.dynamicallyApplyWhenAndroidPluginIsApplied
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.hasKpmModel
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.AndroidTestFragmentLocator
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.SourceSetMappedFragmentLocator

class KotlinAndroidTargetPreset(
    private val project: Project
) : KotlinTargetPreset<KotlinAndroidTarget> {

    override fun getName(): String = PRESET_NAME

    override fun createTarget(name: String): KotlinAndroidTarget {
        val result = KotlinAndroidTarget(name, project).apply {
            disambiguationClassifier = name
            preset = this@KotlinAndroidTargetPreset
        }

        if (project.hasKpmModel)
            SourceSetMappedFragmentLocator.get(project).registerLocator(AndroidTestFragmentLocator(result))

        project.dynamicallyApplyWhenAndroidPluginIsApplied({ result })

        return result
    }

    companion object {
        const val PRESET_NAME = "android"
    }
}
