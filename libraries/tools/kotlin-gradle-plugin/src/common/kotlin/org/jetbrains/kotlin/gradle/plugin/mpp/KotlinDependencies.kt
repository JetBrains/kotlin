/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.model.ObjectFactory
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinDependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinLevelDependenciesDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.gradleVersion
import org.jetbrains.kotlin.gradle.utils.extrasStoredProperty
import javax.inject.Inject

private val MinSupportedGradleVersionWithDependencyCollectors = GradleVersion.version("8.9")

internal val KotlinMultiplatformExtension.dependencies: KotlinLevelDependenciesDslImpl by extrasStoredProperty {
    if (project.gradleVersion < MinSupportedGradleVersionWithDependencyCollectors) {
        throw KotlinTopLevelDependenciesNotAvailable(project.gradleVersion)
    }
    project.objects.newInstance(KotlinLevelDependenciesDslImpl::class.java)
}

internal abstract class KotlinLevelDependenciesDslImpl @Inject internal constructor(
    objectFactory: ObjectFactory
) : KotlinLevelDependenciesDsl {
    val testDependencies: KotlinDependencies = objectFactory.newInstance(KotlinDependencies::class.java)

    override fun test(code: KotlinDependencies.() -> Unit) {
        testDependencies.apply(code)
    }
}

internal class KotlinTopLevelDependenciesNotAvailable(private val currentGradleVersion: GradleVersion): RuntimeException() {
    override val message: String
        get() = "Kotlin top-level dependencies is not available in $currentGradleVersion. Min supported version is $MinSupportedGradleVersionWithDependencyCollectors. " +
                "Please upgrade your Gradle or keep using source-set level dependencies block."
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

    fun wireTopLevelDependencies(dependencies: KotlinDependencies, sourceSet: KotlinSourceSet) {
        dependencies.api wireWith sourceSet.apiConfigurationName
        dependencies.implementation wireWith sourceSet.implementationConfigurationName
        dependencies.compileOnly wireWith sourceSet.compileOnlyConfigurationName
        dependencies.runtimeOnly wireWith sourceSet.runtimeOnlyConfigurationName
    }

    wireTopLevelDependencies(topLevelDependencies, commonMain)
    wireTopLevelDependencies(topLevelDependencies.testDependencies, commonTest)
}