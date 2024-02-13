/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.execution.KotlinAggregateExecutionSource
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer
import org.jetbrains.kotlin.gradle.targets.jvm.JvmCompilationsTestRunSource
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun
import org.jetbrains.kotlin.gradle.utils.*

private val Dependency.isKotlinTestRootDependency: Boolean
    get() = group == KOTLIN_MODULE_GROUP && name == KOTLIN_TEST_ROOT_MODULE_NAME

private val kotlin150Version = SemVer(1.toBigInteger(), 5.toBigInteger(), 0.toBigInteger())

private fun isAtLeast1_5(version: String) = SemVer.fromGradleRichVersion(version) >= kotlin150Version

private val jvmPlatforms = setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)

internal fun Project.configureKotlinTestDependency(
    kotlinExtension: KotlinProjectExtension,
    coreLibrariesVersion: Provider<String>,
) {
    kotlinExtension.forAllTargets { target ->
        target.configureKotlinTestDependency(
            configurations,
            coreLibrariesVersion,
            dependencies,
            tasks
        )
    }
}

private fun KotlinTarget.configureKotlinTestDependency(
    configurations: ConfigurationContainer,
    coreLibrariesVersion: Provider<String>,
    dependencyHandler: DependencyHandler,
    tasks: TaskContainer,
) {
    compilations.configureEach { compilation ->
        val platformType = compilation.platformType
        if (platformType in jvmPlatforms) {
            // Checking dependencies which were added via dependsOn(KotlinSourceSet) call
            // Compilation has own configurations for these that are different from KotlinSourceSet configurations
            KotlinDependencyScope.values()
                .map { configurations.sourceSetDependencyConfigurationByScope(compilation, it) }
                .forEach {
                    it.maybeAddTestDependencyCapability(
                        compilation,
                        coreLibrariesVersion,
                        dependencyHandler,
                        tasks
                    )
                }

            compilation.kotlinSourceSets.forEach { sourceSet ->
                KotlinDependencyScope.values()
                    .map { configurations.sourceSetDependencyConfigurationByScope(sourceSet, it) }
                    .forEach {
                        it.maybeAddTestDependencyCapability(
                            compilation,
                            coreLibrariesVersion,
                            dependencyHandler,
                            tasks
                        )
                    }
            }
        }
    }
}

private fun Configuration.maybeAddTestDependencyCapability(
    compilation: KotlinCompilation<*>,
    coreLibrariesVersion: Provider<String>,
    dependencyHandler: DependencyHandler,
    tasks: TaskContainer,
) {
    withDependencies { dependencies ->
        val testRootDependency = allNonProjectDependencies()
            .singleOrNull { it.isKotlinTestRootDependency }

        if (testRootDependency != null) {
            val depVersion = testRootDependency.version ?: coreLibrariesVersion.get()
            if (!isAtLeast1_5(depVersion)) return@withDependencies

            val testCapability = compilation.kotlinTestCapabilityForJvmSourceSet(tasks)
            if (testCapability != null) {
                dependencies.addLater(
                    testCapability.map { capability ->
                        dependencyHandler
                            .kotlinDependency(KOTLIN_TEST_ROOT_MODULE_NAME, depVersion)
                            .apply {
                                (this as ExternalDependency).capabilities {
                                    it.requireCapability(capability)
                                }
                            }
                    }
                )
            }
        }
    }
}

private fun KotlinCompilation<*>.kotlinTestCapabilityForJvmSourceSet(
    tasks: TaskContainer,
): Provider<String>? {
    val compilationTarget = target

    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
    val testTaskList: List<TaskProvider<out Task>> = when {
        compilationTarget is KotlinTargetWithTests<*, *> -> compilationTarget
            .findTestRunsByCompilation(this)
            .matching { it is KotlinTaskTestRun<*, *> }
            .mapNotNull { (it as KotlinTaskTestRun<*, *>).executionTask }

        compilationTarget is KotlinWithJavaTarget<*, *> &&
                name == KotlinCompilation.TEST_COMPILATION_NAME ->
            listOfNotNull(tasks.locateTask(compilationTarget.testTaskName))

        this is KotlinJvmAndroidCompilation -> when (androidVariant) {
            is DeprecatedAndroidUnitTestVariant -> listOfNotNull(tasks.locateTask(lowerCamelCaseName("test", androidVariant.name)))
            is DeprecatedAndroidTestVariant -> listOfNotNull(androidVariant.connectedInstrumentTestProvider)
            else -> emptyList()
        }

        else -> emptyList()
    }

    if (testTaskList.isEmpty()) return null

    return testTaskList
        .singleOrNull()
        ?.map { task ->
            val framework = when (task) {
                is Test -> testFrameworkOf(task)
                else -> // Android connected test tasks don't inherit from Test, but we use JUnit for them
                    KotlinTestJvmFramework.junit
            }

            "$KOTLIN_MODULE_GROUP:$KOTLIN_TEST_ROOT_MODULE_NAME-framework-$framework"
        }
}

internal const val KOTLIN_TEST_ROOT_MODULE_NAME = "kotlin-test"

private enum class KotlinTestJvmFramework {
    junit, testng, junit5
}

private fun testFrameworkOf(testTask: Test): KotlinTestJvmFramework = when (testTask.options) {
    is JUnitOptions -> KotlinTestJvmFramework.junit
    is JUnitPlatformOptions -> KotlinTestJvmFramework.junit5
    is TestNGOptions -> KotlinTestJvmFramework.testng
    else -> // failed to detect, fallback to junit
        KotlinTestJvmFramework.junit
}

private fun KotlinTargetWithTests<*, *>.findTestRunsByCompilation(
    byCompilation: KotlinCompilation<*>,
): NamedDomainObjectSet<out KotlinTargetTestRun<*>> {
    fun KotlinExecution.ExecutionSource.isProducedFromTheCompilation(): Boolean = when (this) {
        is CompilationExecutionSource<*> -> compilation == byCompilation
        is JvmCompilationsTestRunSource -> byCompilation in testCompilations
        is KotlinAggregateExecutionSource<*> -> this.executionSources.any { it.isProducedFromTheCompilation() }
        else -> false
    }
    return testRuns.matching { it.executionSource.isProducedFromTheCompilation() }
}
