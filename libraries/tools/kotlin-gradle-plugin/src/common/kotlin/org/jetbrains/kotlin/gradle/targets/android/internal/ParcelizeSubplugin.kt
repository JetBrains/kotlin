/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.internal

import com.android.build.api.dsl.KotlinMultiplatformAndroidCompilation
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.utils.configureAndroidVariants

// Use apply plugin: 'kotlin-parcelize' to enable Android Extensions in an Android project.
class ParcelizeSubplugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        val kotlinPluginVersion = target.getKotlinPluginVersion()
        val dependency = target.dependencies.create("org.jetbrains.kotlin:kotlin-parcelize-runtime:$kotlinPluginVersion")
        target.configureAndroidVariants {
            it.runtimeConfiguration.dependencies.add(dependency)
            it.compileConfiguration.dependencies.add(dependency)
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation is KotlinJvmAndroidCompilation || kotlinCompilation is KotlinMultiplatformAndroidCompilation
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList<SubpluginOption>() }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.parcelize"
    override fun getPluginArtifact(): SubpluginArtifact = JetBrainsSubpluginArtifact(artifactId = "kotlin-parcelize-compiler")
}
