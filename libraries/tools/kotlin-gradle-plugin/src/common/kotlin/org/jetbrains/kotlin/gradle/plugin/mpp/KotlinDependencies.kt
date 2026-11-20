/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.jetbrains.kotlin.gradle.dsl.KotlinDependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.utils.extrasStoredProperty
import javax.inject.Inject

internal abstract class KotlinDependenciesImpl @Inject constructor(
    private val ownProject: Project
) : KotlinDependencies {
    override fun kotlin(module: String) = kotlin(module, null)

    // Dependencies.getProject() is deprecated in Gradle 9.5
    override fun kotlin(module: String, version: String?): Dependency = ownProject.dependencyFactory
        .create("org.jetbrains.kotlin", "kotlin-$module", version)
}

internal val KotlinMultiplatformExtension.dependencies: KotlinDependencies by extrasStoredProperty {
    project.objects.newInstance(KotlinDependenciesImpl::class.java, project)
}

internal val ConfigureKotlinTopLevelDependenciesDSL = KotlinProjectSetupAction {
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
