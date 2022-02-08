/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.BaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation

// Use apply plugin: 'kotlin-parcelize' to enable Android Extensions in an Android project.
class ParcelizeSubplugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {

        target.plugins.withType(AndroidSubplugin::class.java) {
            throw GradleException("${target.path}: 'kotlin-parcelize' can't be applied together with 'kotlin-android-extensions'")
        }

        val kotlinPluginVersion = target.getKotlinPluginVersion()
        val dependency = target.dependencies.create("org.jetbrains.kotlin:kotlin-parcelize-runtime:$kotlinPluginVersion")
        target.forEachVariant {
            it.runtimeConfiguration.dependencies.add(dependency)
            it.compileConfiguration.dependencies.add(dependency)
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        if (kotlinCompilation !is KotlinJvmAndroidCompilation) {
            return false
        }

        val project = kotlinCompilation.target.project
        if (project.extensions.findByName("android") !is BaseExtension) {
            return false
        }

        return true
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList<SubpluginOption>() }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.parcelize"
    override fun getPluginArtifact(): SubpluginArtifact = JetBrainsSubpluginArtifact(artifactId = "kotlin-parcelize-compiler")
}
