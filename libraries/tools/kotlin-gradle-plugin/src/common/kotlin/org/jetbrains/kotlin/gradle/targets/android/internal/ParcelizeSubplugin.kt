/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.ParcelizeExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.configureAndroidVariants
import org.jetbrains.kotlin.gradle.utils.getByType
import org.jetbrains.kotlin.gradle.utils.whenMppEnabled

// Use apply plugin: 'kotlin-parcelize' to enable Android Extensions in an Android project.
class ParcelizeSubplugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        val extension = target.extensions.create(EXTENSION_NAME, ParcelizeExtension::class.java)
        extension.implicitlyAddParcelableSupertype.convention(false)
        extension.implicitlyAddParcelableSupertype.finalizeValueOnRead()
        val kotlinPluginVersion = target.getKotlinPluginVersion()
        val dependency = target.dependencies.create("org.jetbrains.kotlin:kotlin-parcelize-runtime:$kotlinPluginVersion")
        // With the implicit supertype, compiled classes implement `kotlinx.parcelize.internal.Parcelable`.
        // The runtime is then part of the ABI, so it must also be on the compile classpath of consumers.
        val abiDependencies = extension.implicitlyAddParcelableSupertype.map { enabled ->
            if (enabled) listOf(dependency) else emptyList()
        }
        var isMultiplatform = false
        target.whenMppEnabled {
            isMultiplatform = true
            target.dependencies.add("commonMainImplementation", dependency)
            target.configurations.getByName("commonMainApi").dependencies.addAllLater(abiDependencies)
        }
        var isApiConfigured = false
        target.configureAndroidVariants {
            if (!isMultiplatform) {
                it.runtimeConfiguration.dependencies.add(dependency)
                it.compileConfiguration.dependencies.add(dependency)
                if (!isApiConfigured) {
                    isApiConfigured = true
                    target.configurations.findByName("api")?.dependencies?.addAllLater(abiDependencies)
                }
            }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = when {
        kotlinCompilation.target.isAndroid() -> true
        // The IDE analyzes shared source sets with the arguments of their metadata compilations. Shared source sets that are
        // compiled for Android need the implicit supertype there, so that Android code sees their `@Parcelize` classes as `Parcelable`.
        // Other metadata compilations, for example of `webMain` or `nativeMain`, do not need the plugin.
        kotlinCompilation is KotlinMetadataCompilation<*> ->
            kotlinCompilation.project.parcelizeExtension.implicitlyAddParcelableSupertype.get() &&
                    kotlinCompilation.isCompiledForAndroid()
        else -> false
    }

    private fun KotlinMetadataCompilation<*>.isCompiledForAndroid(): Boolean =
        if (name == KotlinCompilation.MAIN_COMPILATION_NAME) {
            // The IDE takes the compiler plugin arguments of shared source sets from the `main` metadata compilation.
            // KGP creates it before the source set hierarchy is final, so check the targets of the project instead.
            project.multiplatformExtension.targets.any { it.isAndroid() }
        } else {
            defaultSourceSet.internal.compilations.any { it.target.isAndroid() }
        }

    private fun KotlinTarget.isAndroid(): Boolean =
        // Legacy Android target
        platformType == KotlinPlatformType.androidJvm ||
                // Heuristic for the new Android KMP target. Safe, since the 'android' target name is owned by AGP.
                // FIXME: KT-81384 Provide integration surface for External targets and Gradle Subplugin API
                (this is DecoratedExternalKotlinTarget && name == "android")

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.project.parcelizeExtension.implicitlyAddParcelableSupertype.map { enabled ->
            if (enabled) {
                listOf(SubpluginOption(key = "implicitlyAddParcelableSupertype", value = "true"))
            } else {
                emptyList()
            }
        }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.parcelize"
    override fun getPluginArtifact(): SubpluginArtifact = JetBrainsSubpluginArtifact(artifactId = "kotlin-parcelize-compiler")
}

private const val EXTENSION_NAME = "parcelize"

private val Project.parcelizeExtension: ParcelizeExtension
    get() = extensions.getByType<ParcelizeExtension>()
