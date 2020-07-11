/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.execution.KotlinAggregateExecutionSource
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.jvm.JvmCompilationsTestRunSource
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal fun customizeKotlinDependencies(project: Project) {
    configureStdlibDefaultDependency(project)
    configureKotlinTestDependencies(project)
    configureDefaultVersionsResolutionStrategy(project)
}

private fun configureDefaultVersionsResolutionStrategy(project: Project) {
    project.configurations.all { configuration ->
        // Use the API introduced in Gradle 4.4 to modify the dependencies directly before they are resolved:
        configuration.withDependencies { dependencySet ->
            val coreLibrariesVersion = project.kotlinExtension.coreLibrariesVersion
            dependencySet.filterIsInstance<ExternalDependency>()
                .filter { it.group == KOTLIN_MODULE_GROUP && it.version.isNullOrEmpty() }
                .forEach { it.version { constraint -> constraint.require(coreLibrariesVersion) } }
        }
    }
}

//region stdlib
internal fun configureStdlibDefaultDependency(project: Project) {
    if (!PropertiesProvider(project).stdlibDefaultDependency)
        return

    project.kotlinExtension.sourceSets.all { kotlinSourceSet ->
        val apiConfiguration = project.sourceSetDependencyConfigurationByScope(kotlinSourceSet, KotlinDependencyScope.API_SCOPE)

        apiConfiguration.withDependencies { dependencies ->
            val sourceSetDependencyConfigurations =
                KotlinDependencyScope.values().map { project.sourceSetDependencyConfigurationByScope(kotlinSourceSet, it) }

            val hierarchySourceSetsDependencyConfigurations = kotlinSourceSet.getSourceSetHierarchy().flatMap { hierarchySourceSet ->
                if (hierarchySourceSet === kotlinSourceSet) emptyList() else
                    KotlinDependencyScope.values().map { scope ->
                        project.sourceSetDependencyConfigurationByScope(hierarchySourceSet, scope)
                    }
            }

            val compilations = CompilationSourceSetUtil.compilationsBySourceSets(project).getValue(kotlinSourceSet)
                .filter { it.target !is KotlinMetadataTarget }
            val platformTypes = compilations.mapTo(mutableSetOf()) { it.platformType }

            val sourceSetStdlibPlatformType = when {
                platformTypes.isEmpty() -> KotlinPlatformType.common
                setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm).containsAll(platformTypes) -> KotlinPlatformType.jvm
                platformTypes.size == 1 -> platformTypes.single()
                else -> KotlinPlatformType.common
            }

            val isStdlibAddedByUser = sourceSetDependencyConfigurations
                .flatMap { it.allDependencies }
                .any { dependency -> dependency.group == KOTLIN_MODULE_GROUP && dependency.name in stdlibModules }

            if (!isStdlibAddedByUser) {
                val stdlibModuleName = when (sourceSetStdlibPlatformType) {
                    KotlinPlatformType.jvm, KotlinPlatformType.androidJvm -> jvmStdlibModuleForJvmCompilations(compilations)
                    KotlinPlatformType.js -> "kotlin-stdlib-js"
                    KotlinPlatformType.native -> null
                    KotlinPlatformType.common -> // there's no platform compilation that the source set is default for
                        "kotlin-stdlib-common"
                }

                // If the exact same module is added in the source sets hierarchy, possibly even with a different scope, we don't add it
                val moduleAddedInHierarchy = hierarchySourceSetsDependencyConfigurations.any {
                    it.allDependencies.any { dependency -> dependency.group == KOTLIN_MODULE_GROUP && dependency.name == stdlibModuleName }
                }

                if (stdlibModuleName != null && !moduleAddedInHierarchy)
                    dependencies.add(project.kotlinDependency(stdlibModuleName, project.kotlinExtension.coreLibrariesVersion))
            }
        }
    }
}

private val stdlibModules = setOf("kotlin-stdlib-common", "kotlin-stdlib", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8", "kotlin-stdlib-js")

private fun jvmStdlibModuleForJvmCompilations(compilations: Iterable<KotlinCompilation<*>>): String {
    val jvmTargets = compilations.map { (it.kotlinOptions as KotlinJvmOptions).jvmTarget }
    return if ("1.6" in jvmTargets) "kotlin-stdlib" else "kotlin-stdlib-jdk8"
}
//endregion

//region kotlin-test
internal fun configureKotlinTestDependencies(project: Project) {
    fun isKotlinTestMultiplatformDependency(dependency: Dependency) =
        dependency.group == KOTLIN_MODULE_GROUP && dependency.name == KOTLIN_TEST_MULTIPLATFORM_MODULE_NAME

    KotlinDependencyScope.values().forEach { scope ->
        val versionOrNullBySourceSet = mutableMapOf<KotlinSourceSet, String?>()

        project.kotlinExtension.sourceSets.all { kotlinSourceSet ->
            val configuration = project.sourceSetDependencyConfigurationByScope(kotlinSourceSet, scope)
            var finalizingDependencies = false

            configuration.dependencies.matching(::isKotlinTestMultiplatformDependency).apply {
                firstOrNull()?.let { versionOrNullBySourceSet[kotlinSourceSet] = it.version }
                whenObjectRemoved {
                    if (!finalizingDependencies && !any())
                        versionOrNullBySourceSet.remove(kotlinSourceSet)
                }
                whenObjectAdded { item ->
                    versionOrNullBySourceSet[kotlinSourceSet] = item.version
                }
            }

            configuration.withDependencies { dependencies ->
                val parentOrOwnVersions: List<String?> =
                    kotlinSourceSet.getSourceSetHierarchy().filter(versionOrNullBySourceSet::contains).map(versionOrNullBySourceSet::get)

                finalizingDependencies = true

                parentOrOwnVersions.distinct().forEach { version -> // add dependencies with each version and let Gradle disambiguate them
                    val dependenciesToAdd = kotlinTestDependenciesForSourceSet(project, kotlinSourceSet, version)
                    dependenciesToAdd.filterIsInstance<ExternalDependency>().forEach {
                        if (it.version == null) {
                            it.version { constraint -> constraint.require(project.kotlinExtension.coreLibrariesVersion) }
                        }
                    }
                    dependencies.addAll(dependenciesToAdd)
                    dependencies.removeIf(::isKotlinTestMultiplatformDependency)
                }
            }
        }
    }
}

private fun kotlinTestDependenciesForSourceSet(project: Project, kotlinSourceSet: KotlinSourceSet, version: String?): List<Dependency> {
    val compilations = CompilationSourceSetUtil.compilationsBySourceSets(project).getValue(kotlinSourceSet)
        .filter { it.target !is KotlinMetadataTarget }

    val platformTypes = compilations.mapTo(mutableSetOf()) { it.platformType }

    return when {
        platformTypes.isEmpty() -> emptyList()
        setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm).containsAll(platformTypes) -> listOf(
            kotlinTestDependenciesForJvm(project, compilations, version)
        )
        platformTypes.singleOrNull() == KotlinPlatformType.js -> listOf(
            project.kotlinDependency("kotlin-test-js", version)
        )
        platformTypes.singleOrNull() == KotlinPlatformType.native -> emptyList()
        else -> listOf(
            project.kotlinDependency("kotlin-test-common", version),
            project.kotlinDependency("kotlin-test-annotations-common", version)
        )
    }
}

internal const val KOTLIN_TEST_MULTIPLATFORM_MODULE_NAME = "kotlin-test-multiplatform"

private fun Project.kotlinDependency(moduleName: String, versionOrNull: String?) =
    project.dependencies.create("$KOTLIN_MODULE_GROUP:$moduleName${versionOrNull?.prependIndent(":").orEmpty()}")

private fun kotlinTestDependenciesForJvm(project: Project, compilations: Iterable<KotlinCompilation<*>>, version: String?): Dependency {
    val testTaskLists: List<List<Task>?> = compilations.map { compilation ->
        val target = compilation.target
        when {
            target is KotlinTargetWithTests<*, *> ->
                target.findTestRunsByCompilation(compilation)?.filterIsInstance<KotlinTaskTestRun<*, *>>()?.map { it.executionTask.get() }
            target is KotlinWithJavaTarget<*> ->
                if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME)
                    project.locateTask<AbstractTestTask>(target.testTaskName)?.get()?.let(::listOf)
                else null
            compilation is KotlinJvmAndroidCompilation -> when (compilation.androidVariant) {
                is UnitTestVariant ->
                    project.locateTask<AbstractTestTask>(lowerCamelCaseName("test", compilation.androidVariant.name))?.get()?.let(::listOf)
                is TestVariant -> (compilation.androidVariant as TestVariant).connectedInstrumentTest?.let(::listOf)
                else -> null
            }
            else -> null
        }
    }
    if (null in testTaskLists) {
        return project.kotlinDependency("kotlin-test", version)
    }
    val testTasks = testTaskLists.flatMap { checkNotNull(it) }
    val frameworks = testTasks.mapTo(mutableSetOf()) { testTask ->
        when (testTask) {
            is Test -> testFrameworkOf(testTask)
            else -> // Android connected test tasks don't inherit from Test, but we use JUnit for them
                KotlinTestJvmFramework.junit
        }
    }
    return when {
        frameworks.size > 1 -> project.kotlinDependency("kotlin-test", version)
        else -> project.kotlinDependency("kotlin-test-${frameworks.single().name}", version)
    }
}

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

private fun KotlinTargetWithTests<*, *>.findTestRunsByCompilation(byCompilation: KotlinCompilation<*>): List<KotlinTargetTestRun<*>>? {
    fun KotlinExecution.ExecutionSource.isProducedFromTheCompilation(): Boolean = when (this) {
        is CompilationExecutionSource<*> -> compilation == byCompilation
        is JvmCompilationsTestRunSource -> byCompilation in testCompilations
        is KotlinAggregateExecutionSource<*> -> this.executionSources.any { it.isProducedFromTheCompilation() }
        else -> false
    }
    return testRuns.filter { it.executionSource.isProducedFromTheCompilation() }.takeIf { it.isNotEmpty() }
}
//endregion