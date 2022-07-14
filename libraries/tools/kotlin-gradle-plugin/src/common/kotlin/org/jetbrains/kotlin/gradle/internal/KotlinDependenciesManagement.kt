/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.*
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
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.hasKpmModel
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.kpmModules
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidBaseSourceSetName
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.dependsOnClosure
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer
import org.jetbrains.kotlin.gradle.targets.jvm.JvmCompilationsTestRunSource
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import org.jetbrains.kotlin.gradle.utils.withType

internal const val KOTLIN_MODULE_GROUP = "org.jetbrains.kotlin"
internal const val KOTLIN_COMPILER_EMBEDDABLE = "kotlin-compiler-embeddable"
internal const val PLATFORM_INTEGERS_SUPPORT_LIBRARY = "platform-integers"

internal fun customizeKotlinDependencies(project: Project) {
    val topLevelExtension = project.topLevelExtension
    val propertiesProvider = PropertiesProvider(project)
    val coreLibrariesVersion = project.objects.providerWithLazyConvention {
        topLevelExtension.coreLibrariesVersion
    }

    if (propertiesProvider.stdlibDefaultDependency)
        project.configureStdlibDefaultDependency(topLevelExtension, coreLibrariesVersion)

    if (propertiesProvider.kotlinTestInferJvmVariant) { // TODO: extend this logic to PM20
        project.configureKotlinTestDependency(
            topLevelExtension,
            coreLibrariesVersion,
        )
    }

    project.configurations.configureDefaultVersionsResolutionStrategy(
        coreLibrariesVersion
    )

    excludeStdlibAndKotlinTestCommonFromPlatformCompilations(project)
}

private fun ConfigurationContainer.configureDefaultVersionsResolutionStrategy(
    coreLibrariesVersion: Provider<String>
) = all { configuration ->
    configuration.withDependencies { dependencySet ->
        dependencySet
            .withType<ExternalDependency>()
            .configureEach { dependency ->
                if (dependency.group == KOTLIN_MODULE_GROUP &&
                    dependency.version.isNullOrEmpty()
                ) {
                    dependency.version {
                        it.require(coreLibrariesVersion.get())
                    }
                }
            }
    }
}

private fun excludeStdlibAndKotlinTestCommonFromPlatformCompilations(project: Project) {
    val multiplatformExtension = project.multiplatformExtensionOrNull ?: return

    multiplatformExtension.targets.matching { it !is KotlinMetadataTarget }.configureEach {
        it.excludeStdlibAndKotlinTestCommonFromPlatformCompilations()
    }
}

// there several JVM-like targets, like KotlinWithJava, or KotlinAndroid, and they don't have common supertype
// aside from KotlinTarget
private fun KotlinTarget.excludeStdlibAndKotlinTestCommonFromPlatformCompilations() {
    compilations.all {
        listOfNotNull(
            it.compileDependencyConfigurationName,
            if (!PropertiesProvider(project).experimentalKpmModelMapping)
                it.defaultSourceSet.apiMetadataConfigurationName
            else null,
            if (!PropertiesProvider(project).experimentalKpmModelMapping)
                it.defaultSourceSet.implementationMetadataConfigurationName
            else null,
            (it as? KotlinCompilationToRunnableFiles<*>)?.runtimeDependencyConfigurationName,

            // Additional configurations for (old) jvmWithJava-preset. Remove it when we drop it completely
            (it as? KotlinWithJavaCompilation<*>)?.apiConfigurationName
        ).forEach { configurationName ->
            project.configurations.getByName(configurationName).apply {
                exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-stdlib-common"))
                exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-test-common"))
                exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-test-annotations-common"))
            }
        }
    }
}

//region stdlib
private fun Project.configureStdlibDefaultDependency(
    topLevelExtension: KotlinTopLevelExtension,
    coreLibrariesVersion: Provider<String>
) {

    when {
        project.hasKpmModel -> addStdlibToKpmProject(project, coreLibrariesVersion)
        topLevelExtension is KotlinJsProjectExtension -> topLevelExtension.registerTargetObserver { target ->
            target?.addStdlibDependency(configurations, dependencies, coreLibrariesVersion)
        }
        topLevelExtension is KotlinSingleTargetExtension<*> -> topLevelExtension
            .target
            .addStdlibDependency(configurations, dependencies, coreLibrariesVersion)

        topLevelExtension is KotlinMultiplatformExtension -> topLevelExtension
            .targets
            .configureEach { target ->
                target.addStdlibDependency(configurations, dependencies, coreLibrariesVersion)
            }
    }
}

private fun addStdlibToKpmProject(
    project: Project,
    coreLibrariesVersion: Provider<String>
) {
    project.kpmModules.named(GradleKpmModule.MAIN_MODULE_NAME) { main ->
        main.fragments.named(GradleKpmFragment.COMMON_FRAGMENT_NAME) { common ->
            common.dependencies {
                api(project.dependencies.kotlinDependency("kotlin-stdlib-common", coreLibrariesVersion.get()))
            }
        }
        main.variants.configureEach { variant ->
            val dependencyHandler = project.dependencies
            val stdlibModule = when (variant.platformType) {
                KotlinPlatformType.common -> error("variants are not expected to be common")
                KotlinPlatformType.jvm -> chooseStdlibJvmDependency(coreLibrariesVersion)
                KotlinPlatformType.js -> "kotlin-stdlib-js"
                KotlinPlatformType.wasm -> "kotlin-stdlib-wasm"
                KotlinPlatformType.androidJvm -> null // TODO: expect support on the AGP side?
                KotlinPlatformType.native -> null
            }
            if (stdlibModule != null) {
                variant.dependencies {
                    api(dependencyHandler.kotlinDependency(stdlibModule, coreLibrariesVersion.get()))
                }
            }
        }
    }
}

private fun KotlinTarget.addStdlibDependency(
    configurations: ConfigurationContainer,
    dependencies: DependencyHandler,
    coreLibrariesVersion: Provider<String>
) {
    compilations.configureEach { compilation ->
        compilation.allKotlinSourceSets.forEach { kotlinSourceSet ->
            val scope = if (compilation.isTest() ||
                (this is KotlinAndroidTarget &&
                        kotlinSourceSet.isRelatedToAndroidTestSourceSet()
                        )
            ) {
                KotlinDependencyScope.IMPLEMENTATION_SCOPE
            } else {
                KotlinDependencyScope.API_SCOPE
            }
            val scopeConfiguration = configurations
                .sourceSetDependencyConfigurationByScope(kotlinSourceSet, scope)

            scopeConfiguration.withDependencies { dependencySet ->
                // Check if stdlib is directly added to SourceSet
                if (isStdlibAddedByUser(configurations, stdlibModules, kotlinSourceSet)) return@withDependencies

                val stdlibModule = compilation
                    .platformType
                    .stdlibPlatformType(coreLibrariesVersion, this, kotlinSourceSet)
                    ?: return@withDependencies

                // Check if stdlib module is added to SourceSets hierarchy
                if (
                    isStdlibAddedByUser(
                        configurations,
                        setOf(stdlibModule),
                        *kotlinSourceSet.dependsOnClosure.toTypedArray()
                    )
                ) return@withDependencies

                dependencySet.addLater(
                    coreLibrariesVersion.map {
                        dependencies.kotlinDependency(stdlibModule, it)
                    }
                )
            }
        }
    }
}

private fun isStdlibAddedByUser(
    configurations: ConfigurationContainer,
    stdlibModules: Set<String>,
    vararg sourceSets: KotlinSourceSet
): Boolean {
    return sourceSets
        .asSequence()
        .flatMap { sourceSet ->
            KotlinDependencyScope.values().map { scope ->
                configurations.sourceSetDependencyConfigurationByScope(sourceSet, scope)
            }.asSequence()
        }
        .flatMap { it.allNonProjectDependencies().asSequence() }
        .any { dependency ->
            dependency.group == KOTLIN_MODULE_GROUP && dependency.name in stdlibModules
        }
}

private fun KotlinPlatformType.stdlibPlatformType(
    coreLibrariesVersion: Provider<String>,
    kotlinTarget: KotlinTarget,
    kotlinSourceSet: KotlinSourceSet
): String? = when (this) {
    KotlinPlatformType.jvm -> chooseStdlibJvmDependency(coreLibrariesVersion)
    KotlinPlatformType.androidJvm -> {
        if (kotlinTarget is KotlinAndroidTarget &&
            kotlinSourceSet.androidSourceSetInfoOrNull?.androidSourceSetName == AndroidBaseSourceSetName.Main.name
        ) {
            chooseStdlibJvmDependency(coreLibrariesVersion)
        } else {
            null
        }
    }

    KotlinPlatformType.js -> "kotlin-stdlib-js"
    KotlinPlatformType.wasm -> "kotlin-stdlib-wasm"
    KotlinPlatformType.native -> null
    KotlinPlatformType.common -> // there's no platform compilation that the source set is default for
        "kotlin-stdlib-common"
}

private val kotlin180Version = SemVer(1.toBigInteger(), 8.toBigInteger(), 0.toBigInteger())

private fun chooseStdlibJvmDependency(
    coreLibrariesVersion: Provider<String>
): String {
    // Current 'SemVer.satisfies' release always returns `false` for any "-SNAPSHOT" version.
    return if (SemVer.from(coreLibrariesVersion.get()) < kotlin180Version) {
        "kotlin-stdlib-jdk8"
    } else {
        "kotlin-stdlib"
    }
}

private val androidTestVariants = setOf(AndroidVariantType.UnitTest, AndroidVariantType.InstrumentedTest)

private fun KotlinSourceSet.isRelatedToAndroidTestSourceSet(): Boolean {
    val androidVariant = androidSourceSetInfoOrNull?.androidVariantType ?: return false
    return androidVariant in androidTestVariants
}

private val stdlibModules = setOf("kotlin-stdlib-common", "kotlin-stdlib", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8", "kotlin-stdlib-js")
//endregion

//region kotlin-test
private val Dependency.isKotlinTestRootDependency: Boolean
    get() = group == KOTLIN_MODULE_GROUP && name == KOTLIN_TEST_ROOT_MODULE_NAME

private val kotlin150Version = SemVer(1.toBigInteger(), 5.toBigInteger(), 0.toBigInteger())

private fun isAtLeast1_5(version: String) = SemVer.from(version) >= kotlin150Version

private val jvmPlatforms = setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)

private fun Project.configureKotlinTestDependency(
    topLevelExtension: KotlinTopLevelExtension,
    coreLibrariesVersion: Provider<String>,
) {
    when (topLevelExtension) {
        is KotlinJsProjectExtension -> topLevelExtension.registerTargetObserver { target ->
            target?.configureKotlinTestDependency(
                configurations,
                coreLibrariesVersion,
                objects,
                dependencies,
                tasks
            )
        }

        is KotlinSingleTargetExtension<*> -> topLevelExtension.target.configureKotlinTestDependency(
            configurations,
            coreLibrariesVersion,
            dependencies,
            tasks
        )

        is KotlinMultiplatformExtension -> topLevelExtension.targets.configureEach { target ->
            target.configureKotlinTestDependency(
                configurations,
                coreLibrariesVersion,
                dependencies,
                tasks
            )
        }
    }
}

private fun KotlinTarget.configureKotlinTestDependency(
    configurations: ConfigurationContainer,
    coreLibrariesVersion: Provider<String>,
    dependencyHandler: DependencyHandler,
    tasks: TaskContainer
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
    tasks: TaskContainer
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
    tasks: TaskContainer
): Provider<String>? {
    val compilationTarget = target
    val testTaskList: List<TaskProvider<out Task>> = when {
        compilationTarget is KotlinTargetWithTests<*, *> -> compilationTarget
            .findTestRunsByCompilation(this)
            .matching { it is KotlinTaskTestRun<*, *> }
            .mapNotNull { (it as KotlinTaskTestRun<*, *>).executionTask }

        compilationTarget is KotlinWithJavaTarget<*> &&
                name == KotlinCompilation.TEST_COMPILATION_NAME ->
            listOfNotNull(tasks.locateTask(compilationTarget.testTaskName))

        this is KotlinJvmAndroidCompilation -> when (androidVariant) {
            is UnitTestVariant -> listOfNotNull(tasks.locateTask(lowerCamelCaseName("test", androidVariant.name)))
            is TestVariant -> listOfNotNull((androidVariant as TestVariant).connectedInstrumentTestProvider)
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
    byCompilation: KotlinCompilation<*>
): NamedDomainObjectSet<out KotlinTargetTestRun<*>> {
    fun KotlinExecution.ExecutionSource.isProducedFromTheCompilation(): Boolean = when (this) {
        is CompilationExecutionSource<*> -> compilation == byCompilation
        is JvmCompilationsTestRunSource -> byCompilation in testCompilations
        is KotlinAggregateExecutionSource<*> -> this.executionSources.any { it.isProducedFromTheCompilation() }
        else -> false
    }
    return testRuns.matching { it.executionSource.isProducedFromTheCompilation() }
}
//endregion

internal fun DependencyHandler.kotlinDependency(moduleName: String, versionOrNull: String?) =
    create("$KOTLIN_MODULE_GROUP:$moduleName${versionOrNull?.prependIndent(":").orEmpty()}")

private fun Configuration.allNonProjectDependencies() = allDependencies.matching { it !is ProjectDependency }
