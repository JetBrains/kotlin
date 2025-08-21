/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.Dependency
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
 * - [org.gradle.api.artifacts.dsl.Dependencies] is accessible since 7.6
 * - [org.gradle.api.artifacts.dsl.DependencyCollector] is accessible since 8.6
 * - [org.gradle.api.artifacts.Configuration.fromDependencyCollector] is accessible since 8.7
 * - [org.jetbrains.kotlin.gradle.dsl.KotlinDependencies] can't be instantiated with [org.gradle.api.artifacts.dsl.DependencyCollector] before 8.8
 *
 * Keep in sync with [org.jetbrains.kotlin.gradle.dsl.MinSupportedGradleVersionWithDependencyCollectorsConst]
 */
internal const val MinSupportedGradleVersionWithDependencyCollectorsConst = "8.8"
internal val MinSupportedGradleVersionWithDependencyCollectors = GradleVersion.version(MinSupportedGradleVersionWithDependencyCollectorsConst)

internal sealed class KotlinTopLevelDependenciesBlock {
    class Dependencies(val block: KotlinDependencies) : KotlinTopLevelDependenciesBlock()
    object UnavailableInCurrentGradleVersion : KotlinTopLevelDependenciesBlock()
}

internal abstract class KotlinDependenciesImpl : KotlinDependencies {
    override fun kotlin(module: String) = kotlin(module, null)
    override fun kotlin(module: String, version: String?): Dependency = project.dependencyFactory
        .create("org.jetbrains.kotlin", "kotlin-$module", version)
}

internal val KotlinMultiplatformExtension.dependencies: KotlinTopLevelDependenciesBlock by extrasStoredProperty {
    if (project.gradleVersion < MinSupportedGradleVersionWithDependencyCollectors) {
        KotlinTopLevelDependenciesBlock.UnavailableInCurrentGradleVersion
    } else {
        KotlinTopLevelDependenciesBlock.Dependencies(project.objects.newInstance(KotlinDependenciesImpl::class.java))
    }
}

internal val ConfigureKotlinTopLevelDependenciesDSL = KotlinProjectSetupAction {
    val topLevelDependencies = when (val dependencies = project.multiplatformExtension.dependencies) {
        KotlinTopLevelDependenciesBlock.UnavailableInCurrentGradleVersion -> return@KotlinProjectSetupAction
        is KotlinTopLevelDependenciesBlock.Dependencies -> dependencies.block
    }

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