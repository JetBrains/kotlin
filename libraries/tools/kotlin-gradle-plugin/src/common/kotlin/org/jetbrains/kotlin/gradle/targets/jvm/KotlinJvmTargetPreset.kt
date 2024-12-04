/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTargetConfigurator

@DeprecatedTargetPresetApi
class KotlinJvmTargetPreset(
    project: Project
) : KotlinOnlyTargetPreset<KotlinJvmTarget, KotlinJvmCompilation>(
    project
) {
    override fun instantiateTarget(name: String): KotlinJvmTarget {
        return project.objects.newInstance(KotlinJvmTarget::class.java, project)
    }

    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactory(forTarget: KotlinJvmTarget): KotlinCompilationFactory<KotlinJvmCompilation> =
        KotlinJvmCompilationFactory(forTarget)

    override fun createKotlinTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinJvmTarget> =
        KotlinJvmTargetConfigurator()

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm

    companion object {
        const val PRESET_NAME = "jvm"
    }
}
