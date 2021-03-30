/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.execution.KotlinAggregateExecutionSource
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.withAllDependsOnSourceSets
import org.jetbrains.kotlin.gradle.plugin.sources.resolveAllDependsOnSourceSets
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.jvm.JvmCompilationsTestRunSource
import org.jetbrains.kotlin.gradle.tasks.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal fun customizeKotlinDependencies(project: Project) {
    configureStdlibDefaultDependency(project)
    configureKotlinTestDependency(project)
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
internal fun configureStdlibDefaultDependency(project: Project) = with(project) {
    if (!PropertiesProvider(project).stdlibDefaultDependency)
        return

    val scopesToHandleConfigurations = listOf(KotlinDependencyScope.API_SCOPE, KotlinDependencyScope.IMPLEMENTATION_SCOPE)

    project.kotlinExtension.sourceSets.all { kotlinSourceSet ->
        scopesToHandleConfigurations.forEach { scope ->
            val scopeConfiguration = project.sourceSetDependencyConfigurationByScope(kotlinSourceSet, scope)

            project.tryWithDependenciesIfUnresolved(scopeConfiguration) { dependencies ->
                val scopeToAddStdlibDependency =
                    if (isRelatedToAndroidTestSourceSet(project, kotlinSourceSet)) // AGP deprecates API configurations in test source sets
                        KotlinDependencyScope.IMPLEMENTATION_SCOPE
                    else KotlinDependencyScope.API_SCOPE

                if (scope != scopeToAddStdlibDependency)
                    return@tryWithDependenciesIfUnresolved

                chooseAndAddStdlibDependency(project, kotlinSourceSet, dependencies)
            }
        }
    }
}

private fun chooseAndAddStdlibDependency(
    project: Project,
    kotlinSourceSet: KotlinSourceSet,
    dependencies: DependencySet
) {
    val sourceSetDependencyConfigurations =
        KotlinDependencyScope.values().map { project.sourceSetDependencyConfigurationByScope(kotlinSourceSet, it) }

    val hierarchySourceSetsDependencyConfigurations = kotlinSourceSet.resolveAllDependsOnSourceSets().flatMap { hierarchySourceSet ->
        KotlinDependencyScope.values().map { scope ->
            project.sourceSetDependencyConfigurationByScope(hierarchySourceSet, scope)
        }
    }

    val compilations = CompilationSourceSetUtil.compilationsBySourceSets(project).getValue(kotlinSourceSet)
        .filter { it.target !is KotlinMetadataTarget }

    if (compilations.isEmpty())
    // source set doesn't take part in any compilation; prohibit this in the future; also, this caused KT-40559 in Android libraries
        return

    val platformTypes = compilations.mapTo(mutableSetOf()) { it.platformType }

    val sourceSetStdlibPlatformType = when {
        platformTypes.size == 1 -> platformTypes.single()
        setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm).containsAll(platformTypes) -> KotlinPlatformType.jvm
        else -> KotlinPlatformType.common
    }

    val isStdlibAddedByUser = sourceSetDependencyConfigurations
        .flatMap { it.allDependencies }
        .any { dependency -> dependency.group == KOTLIN_MODULE_GROUP && dependency.name in stdlibModules }

    if (!isStdlibAddedByUser) {
        val stdlibModuleName = when (sourceSetStdlibPlatformType) {
            KotlinPlatformType.jvm -> stdlibModuleForJvmCompilations(compilations)
            KotlinPlatformType.androidJvm ->
                if (kotlinSourceSet.name == androidMainSourceSetName(project)) stdlibModuleForJvmCompilations(compilations) else null
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

private fun Project.findAndroidTarget(): KotlinAndroidTarget? {
    val kotlinExtension = project.kotlinExtension
    return when (kotlinExtension) {
        is KotlinMultiplatformExtension -> kotlinExtension.targets.withType(KotlinAndroidTarget::class.java).single()
        is KotlinAndroidProjectExtension -> kotlinExtension.target
        else -> null
    }
}

private fun androidMainSourceSetName(project: Project): String {
    val target = project.findAndroidTarget() ?: error("No Android target found")
    return AbstractAndroidProjectHandler.kotlinSourceSetNameForAndroidSourceSet(target, "main")
}

private fun isRelatedToAndroidTestSourceSet(project: Project, kotlinSourceSet: KotlinSourceSet): Boolean {
    val androidExtension = project.extensions.findByName("android") as? TestedExtension ?: return false
    val androidTarget = project.findAndroidTarget() ?: return false

    if (androidTarget.compilations.any {
            (it.androidVariant is UnitTestVariant || it.androidVariant is TestVariant) && it.defaultSourceSet === kotlinSourceSet
        }
    ) return true

    (androidExtension.testVariants + androidExtension.unitTestVariants).forEach { variant ->
        if (variant.sourceSets.any {
                kotlinSourceSet.name == AbstractAndroidProjectHandler.kotlinSourceSetNameForAndroidSourceSet(androidTarget, it.name)
            }
        ) return true
    }

    return false
}

private val stdlibModules = setOf("kotlin-stdlib-common", "kotlin-stdlib", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8", "kotlin-stdlib-js")

private fun stdlibModuleForJvmCompilations(compilations: Iterable<KotlinCompilation<*>>): String {
    val jvmTargets = compilations.map { (it.kotlinOptions as KotlinJvmOptions).jvmTarget }
    return if ("1.6" in jvmTargets) "kotlin-stdlib" else "kotlin-stdlib-jdk8"
}
//endregion

//region kotlin-test
internal fun configureKotlinTestDependency(project: Project) {
    if (!isGradleVersionAtLeast(6, 0))
        return
    if (!PropertiesProvider(project).kotlinTestInferJvmVariant)
        return

    fun isKotlinTestRootDependency(dependency: Dependency) =
        dependency.group == KOTLIN_MODULE_GROUP && dependency.name == KOTLIN_TEST_ROOT_MODULE_NAME

    val versionPrefixRegex = Regex("""^(\d+)\.(\d+)""")
    fun isAtLeast1_5(version: String) = versionPrefixRegex.find(version)?.let {
        val c1 = it.groupValues[1].toInt()
        val c2 = it.groupValues[2].toInt()
        c1 > 1 || c1 == 1 && c2 >= 5
    } ?: false

    KotlinDependencyScope.values().forEach { scope ->
        val versionOrNullBySourceSet = mutableMapOf<KotlinSourceSet, String?>()

        project.kotlinExtension.sourceSets.all { kotlinSourceSet ->
            val configuration = project.sourceSetDependencyConfigurationByScope(kotlinSourceSet, scope)
            var finalizingDependencies = false

            configuration.dependencies.matching(::isKotlinTestRootDependency).apply {
                firstOrNull()?.let { versionOrNullBySourceSet[kotlinSourceSet] = it.version }
                whenObjectRemoved {
                    if (!finalizingDependencies && !any())
                        versionOrNullBySourceSet.remove(kotlinSourceSet)
                }
                whenObjectAdded { item ->
                    versionOrNullBySourceSet[kotlinSourceSet] = item.version
                }
            }

            project.tryWithDependenciesIfUnresolved(configuration) { dependencies ->
                val parentOrOwnVersions: List<String?> =
                    kotlinSourceSet.withAllDependsOnSourceSets().filter(versionOrNullBySourceSet::contains).map(versionOrNullBySourceSet::get)

                finalizingDependencies = true

                for (version in parentOrOwnVersions.distinct()) {  // add dependencies with each version and let Gradle disambiguate them
                    val effectiveVersion = version ?: project.kotlinExtension.coreLibrariesVersion
                    if (!isAtLeast1_5(effectiveVersion)) continue
                    val clarifyCapability = kotlinTestCapabilityForJvmSourceSet(project, kotlinSourceSet) ?: continue
                    val clarifiedDependency =
                        (project.kotlinDependency(KOTLIN_TEST_ROOT_MODULE_NAME, version) as ExternalDependency).apply {
                            if (version == null) {
                                version { constraint -> constraint.require(project.kotlinExtension.coreLibrariesVersion) }
                            }
                            capabilities {
                                it.requireCapability(clarifyCapability)
                            }
                        }
                    dependencies.add(clarifiedDependency)
                }
            }
        }
    }
}

private fun kotlinTestCapabilityForJvmSourceSet(project: Project, kotlinSourceSet: KotlinSourceSet): String? {
    val compilations = CompilationSourceSetUtil.compilationsBySourceSets(project).getValue(kotlinSourceSet)
        .filter { it.target !is KotlinMetadataTarget }

    val platformTypes = compilations.map { it.platformType }
    // TODO: Extract jvmPlatformTypes to public constant?
    val jvmPlatforms = setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)
    if (!jvmPlatforms.containsAll(platformTypes)) return null

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
        return null
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
        frameworks.size > 1 -> null
        else -> "$KOTLIN_MODULE_GROUP:$KOTLIN_TEST_ROOT_MODULE_NAME-framework-${frameworks.single()}"
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

private fun Project.kotlinDependency(moduleName: String, versionOrNull: String?) =
    project.dependencies.create("$KOTLIN_MODULE_GROUP:$moduleName${versionOrNull?.prependIndent(":").orEmpty()}")

private fun Project.tryWithDependenciesIfUnresolved(configuration: Configuration, action: (DependencySet) -> Unit) {
    fun reportAlreadyResolved() {
        logger.info("Could not setup Kotlin-specific dependencies for $configuration as it is already resolved")
    }

    if (configuration.state != Configuration.State.UNRESOLVED)
        return reportAlreadyResolved()

    // Gradle doesn't provide any public API to check if it's safe to modify a configuration's dependencies.
    // The state of the configuration may still be UNRESOLVED but it might have 'participated' in dependency resolution and its dependencies
    // may now be frozen.
    try {
        configuration.withDependencies(action)
    } catch (e: InvalidUserDataException) {
        reportAlreadyResolved()
    }
}
