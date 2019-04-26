/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTargetConfigurator

class KotlinJvmTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinJvmTarget, KotlinJvmCompilation>(
    project,
    kotlinPluginVersion
) {
    override fun instantiateTarget(): KotlinJvmTarget = KotlinJvmTarget(project)

    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJvmCompilation>): KotlinCompilationFactory<KotlinJvmCompilation> =
        KotlinJvmCompilationFactory(forTarget)

    override fun createKotlinTargetConfigurator() = KotlinJvmTargetConfigurator(kotlinPluginVersion)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm

    companion object {
        const val PRESET_NAME = "jvm"
    }
}
