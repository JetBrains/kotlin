/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPlugin.Companion.dynamicallyApplyWhenAndroidPluginIsApplied
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset

import javax.inject.Inject

abstract class KotlinAndroidTargetPreset @Inject constructor(
    private val project: Project
) : KotlinTargetPreset<KotlinAndroidTarget> {

    override fun getName(): String = PRESET_NAME

    override fun createTarget(name: String): KotlinAndroidTarget {
        val result = project.objects.newInstance(
            KotlinAndroidTarget::class.java,
            name,
            project
        ).apply {
            preset = this@KotlinAndroidTargetPreset
            targetUnderConstruction = this
        }

        project.dynamicallyApplyWhenAndroidPluginIsApplied({ result })

        targetUnderConstruction = null

        return result
    }

    /** This is a way to check if there's an Android target being configured now despite it not being added to the `kotlin.targets` yet */
    internal var targetUnderConstruction: KotlinAndroidTarget? = null

    companion object {
        const val PRESET_NAME = "android"
    }
}
