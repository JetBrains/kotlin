/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinDependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.gradleVersion
import org.jetbrains.kotlin.gradle.utils.extrasStoredProperty

/**
 * FIXME: Why was it 8.9?
 *
 * - [org.gradle.api.artifacts.dsl.Dependencies] is accessible since 7.6
 * - [org.gradle.api.artifacts.dsl.DependencyCollector] is accessible since 8.6
 * - [org.gradle.api.artifacts.Configuration.fromDependencyCollector] is accessible since 8.7
 * [org.jetbrains.kotlin.gradle.dsl.KotlinDependencies] can't be instantiated
 * with [org.gradle.api.artifacts.dsl.DependencyCollector] before 8.8
 */
internal const val MinSupportedGradleVersionWithDependencyCollectorsString = "8.8"
internal val MinSupportedGradleVersionWithDependencyCollectors = GradleVersion.version(MinSupportedGradleVersionWithDependencyCollectorsString)

internal val KotlinMultiplatformExtension.dependencies: KotlinDependencies by extrasStoredProperty {
    if (project.gradleVersion < MinSupportedGradleVersionWithDependencyCollectors) {
        throw KotlinTopLevelDependenciesNotAvailable(project.gradleVersion)
    }
    project.objects.newInstance(KotlinDependencies::class.java)
}

internal class KotlinTopLevelDependenciesNotAvailable(
    currentGradleVersion: GradleVersion,
): RuntimeException() {
    private val currentGradleVersionString = currentGradleVersion.toString()
    private val minimumSupportedGradleVersion = MinSupportedGradleVersionWithDependencyCollectors.toString()

    override val message: String
        get() = "Kotlin top-level dependencies is not available in $currentGradleVersionString. Minimum supported version is $minimumSupportedGradleVersion. " +
                "Please upgrade your Gradle version or keep using source-set level dependencies block: https://kotl.in/kmp-top-level-dependencies"
}

internal val ConfigureKotlinTopLevelDependenciesDSL = KotlinProjectSetupAction {
    if (project.gradleVersion < MinSupportedGradleVersionWithDependencyCollectors) return@KotlinProjectSetupAction

    val topLevelDependencies = project.multiplatformExtension.dependencies
    val commonMain = project.multiplatformExtension.sourceSets.commonMain.get()
    val commonTest = project.multiplatformExtension.sourceSets.commonTest.get()

    infix fun DependencyCollector.wireWith(configurationName: String) {
        val configuration = project.configurations.getByName(configurationName)
        configuration.fromDependencyCollector(this)
    }

    topLevelDependencies.api wireWith commonMain.apiConfigurationName
    topLevelDependencies.implementation wireWith commonMain.implementationConfigurationName
    topLevelDependencies.compileOnly wireWith commonMain.compileOnlyConfigurationName
    topLevelDependencies.runtimeOnly wireWith commonMain.runtimeOnlyConfigurationName

    topLevelDependencies.testImplementation wireWith commonTest.implementationConfigurationName
    topLevelDependencies.testCompileOnly wireWith commonTest.compileOnlyConfigurationName
    topLevelDependencies.testRuntimeOnly wireWith commonTest.runtimeOnlyConfigurationName
}