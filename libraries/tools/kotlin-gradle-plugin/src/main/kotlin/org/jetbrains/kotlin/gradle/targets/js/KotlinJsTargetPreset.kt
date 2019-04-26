/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTargetConfigurator

open class KotlinJsTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinOnlyTarget<KotlinJsCompilation>, KotlinJsCompilation>(
    project,
    kotlinPluginVersion
) {
    override fun instantiateTarget() = KotlinOnlyTarget<KotlinJsCompilation>(project, KotlinPlatformType.js)

    override fun createKotlinTargetConfigurator() = KotlinJsTargetConfigurator(kotlinPluginVersion)

    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJsCompilation>) =
        KotlinJsCompilationFactory(project, forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    companion object {
        const val PRESET_NAME = "js"
    }
}

class KotlinJsSingleTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) :
    KotlinJsTargetPreset(project, kotlinPluginVersion) {

    // In a Kotlin/JS single-platform project, we don't need any disambiguation suffixes or prefixes in the names:
    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsCompilation>): String? = null
}