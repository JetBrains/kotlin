/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.getValue
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.ignoreAccessViolations
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.setupMavenPublication
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.gradle.util.resolveRepoArtifactPath
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.collections.map
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@MppGradlePluginTests
@DisplayName("Separate KMP compilation a.k.a. the new KMP compilation scheme: KT-77546")
class SeparateKmpCompilationIT : KGPBaseTest() {
    @DisplayName("fragment dependencies are not duplicated if they are defined higher in the hierarchy")
    @GradleTest
    fun fragmentDependenciesAreDeduplicated(gradleVersion: GradleVersion) {
        doTestFragmentDependenciesArg(gradleVersion, additionalProjectConfiguration = {
            applyMultiplatform {
                sourceSets {
                    val jvmAndJs by it.creating {
                        dependsOn(it.commonMain.get())
                    }
                    it.jvmMain {
                        dependsOn(jvmAndJs)
                    }
                    it.jsMain {
                        dependsOn(jvmAndJs)
                    }
                }
            }
        }) { fragmentDependencies ->
            val visitedDependencies = mutableSetOf<String>()
            for ((_, dependencies) in fragmentDependencies) {
                for (dependency in dependencies) {
                    assert(visitedDependencies.add(dependency)) {
                        "Duplicate dependency '$dependency' found in fragment dependencies: $fragmentDependencies"
                    }
                }
            }
        }
    }

    @DisplayName("native stdlib and platform dependencies are added to fragment dependencies")
    @GradleTest
    fun nativeStdlibIsAdded(gradleVersion: GradleVersion) {
        doTestFragmentDependenciesArg(gradleVersion, listOf("linuxX64")) { fragmentDependenciesPerFragment ->
            val nativeDependencies = fragmentDependenciesPerFragment.getValue("nativeMain")
            assert(nativeDependencies.count { "stdlib" in it } == 1 && nativeDependencies.any { "/klib/common/stdlib" in it }) {
                "Exactly one K/N stdlib dependency is expected in nativeMain: $nativeDependencies"
            }
            assert(nativeDependencies.filter { "stdlib" !in it }.all { "(linux_arm64, linux_x64, macos_arm64)" in it }) {
                "nativeMain dependencies are expected contain only commonized platform libraries and stdlib"
            }
            val commonDependencies = fragmentDependenciesPerFragment.getValue("commonMain")
            assert(commonDependencies.count { "stdlib" in it } == 1 && commonDependencies.none { "/klib/common/stdlib" in it }) {
                "No K/N stdlib dependency is expected in commonMain: $commonDependencies"
            }
            assert(commonDependencies.none { "commonized" in it }) {
                "No commonized platform libraries are expected in commonMain because common is not native only: $commonDependencies"
            }
            val linuxDependencies = fragmentDependenciesPerFragment.getValue("linuxMain")
            assert(linuxDependencies.all { "(linux_arm64, linux_x64)" in it }) {
                "Expected linuxMain to contain only commonized platform libraries"
            }
            assert(fragmentDependenciesPerFragment.keys.size == 3) {
                "Unexpected fragment dependencies: $fragmentDependenciesPerFragment"
            }
        }
    }

    private fun doTestFragmentDependenciesArg(
        gradleVersion: GradleVersion,
        targetsToRun: List<String> = listOf("linuxX64", "jvm", "js"),
        additionalProjectConfiguration: Project.() -> Unit = {},
        assertions: (Map<String, List<String>>) -> Unit,
    ) {
        defaultProject(gradleVersion, additionalProjectConfiguration) {
            @Suppress("DEPRECATION")
            val compileArgs: List<Pair<String, CommonCompilerArguments>> = providerBuildScriptReturn {
                val targets = targetsToRun.map { kotlinMultiplatform.targets.getByName(it) }
                project.provider {
                    targets.map { target ->
                        val task = target.compilations.getByName("main").compileTaskProvider.get()
                        project.ignoreAccessViolations {
                            task as KotlinCompilerArgumentsProducer
                            target.name to task.createCompilerArguments() as CommonCompilerArguments
                        }
                    }
                }
            }.buildAndReturn(
                *targetsToRun.map { ":compileKotlin${it.capitalize()}" }.toTypedArray(),
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED, // otherwise we would access GMT task outputs before the task execution
            )
            for ((targetName, particularCompileArgs) in compileArgs) {
                val fragmentDependencies = particularCompileArgs.fragmentDependencies
                assert(fragmentDependencies != null) { "Fragment dependencies are not set for $targetName" }
                val dependenciesPerFragment = fragmentDependencies!!
                    .map { it.replace('\\', '/') }
                    .groupBy({ it.substringBefore(":") }) { it.substringAfter(":") }
                assertions(dependenciesPerFragment)
            }
        }
    }

    @DisplayName("inaccessible symbols: local kmp library -> compile consumer")
    @GradleTest
    fun localKmpConsumer(gradleVersion: GradleVersion) {
        doTestInaccessibleSymbols(gradleVersion)
    }

    @DisplayName("inaccessible symbols: published kmp library -> compile consumer")
    @GradleTest
    fun remoteKmpConsumer(gradleVersion: GradleVersion, @TempDir localRepository: Path) {
        doTestInaccessibleSymbols(gradleVersion, localRepository)
    }

    private fun doTestInaccessibleSymbols(gradleVersion: GradleVersion, localRepository: Path? = null) {
        project(
            "empty",
            gradleVersion,
            localRepoDir = localRepository,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjects(),
        ) {
            plugins {
                kotlin("multiplatform")
            }
            val sourceSetNames = setOfNotNull(
                "commonMain",
                "jvmMain",
                "jsMain",
                "linuxArm64Main",
                "linuxX64Main",
                "nativeMain",
            )
            val repositoryPath = localRepository?.absolutePathString()
            val depGroup = if (localRepository != null) PUBLISHED_DEP_GROUP else null
            val depVersion = if (localRepository != null) PUBLISHED_DEP_VERSION else null
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        jvm()
                        js()
                        linuxArm64()
                        linuxX64()

                        with(sourceSets) {
                            commonMain {
                                dependencies {
                                    implementation(if (repositoryPath != null) "$depGroup:library:$depVersion" else project(":library"))
                                }
                                nativeMain.get() // force creating nativeMain, because it's usually created later
                                compileSource(
                                    """
                                    |fun main() {
                                    |    ${
                                        sourceSetNames.map { named(it) }.joinToString("\n    ") { sourceSet -> "${sourceSet.name}()" }
                                    }
                                    |}
                                """.trimMargin()
                                )
                            }
                        }
                    }
                }
            }

            enableSeparateCompilation()

            val librarySubproject = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                        js()
                        linuxArm64()
                        linuxX64()
                        with(sourceSets) {
                            nativeMain.get() // force creating nativeMain, because it's usually created later
                            for (sourceSet in sourceSetNames.map { named(it) }) {
                                sourceSet.get().compileStubSourceWithSourceSetName()
                            }
                        }
                    }
                    if (repositoryPath != null) {
                        require(depGroup != null && depVersion != null)
                        project.setupMavenPublication(
                            "Stub",
                            PublisherConfiguration(depGroup, depVersion, repositoryPath)
                        )
                    }
                }
            }

            include(librarySubproject, "library")

            if (localRepository != null) {
                build(":library:publish")
            }

            buildAndFail(
                ":assemble",
                buildOptions = defaultBuildOptions.copy(continueAfterFailure = true)
            ) {
                // ensures no unexpected task dependencies are added
                val libraryTasks = setOf(
                    ":library:allMetadataJar",
                    ":library:kmpPartiallyResolvedDependenciesChecker",
                    ":library:checkKotlinGradlePluginConfigurationErrors",
                    ":library:commonizeNativeDistribution",
                    ":library:compileCommonMainKotlinMetadata",
                    ":library:compileJvmMainJava",
                    ":library:compileKotlinJs",
                    ":library:compileKotlinJvm",
                    ":library:compileKotlinLinuxArm64",
                    ":library:compileKotlinLinuxX64",
                    ":library:compileLinuxMainKotlinMetadata",
                    ":library:compileNativeMainKotlinMetadata",
                    ":library:exportCommonSourceSetsMetadataLocationsForMetadataApiElements",
                    ":library:exportRootPublicationCoordinatesForMetadataApiElements",
                    ":library:exportCrossCompilationMetadataForLinuxArm64ApiElements",
                    ":library:exportCrossCompilationMetadataForLinuxX64ApiElements",
                    ":library:generateProjectStructureMetadata",
                    ":library:generateSourceIn_commonMain_0",
                    ":library:generateSourceIn_jsMain_2",
                    ":library:generateSourceIn_jvmMain_1",
                    ":library:generateSourceIn_linuxArm64Main_3",
                    ":library:generateSourceIn_linuxX64Main_4",
                    ":library:generateSourceIn_nativeMain_5",
                    ":library:jvmJar",
                    ":library:jvmMainClasses",
                    ":library:jvmProcessResources",
                    ":library:metadataCommonMainClasses",
                    ":library:metadataCommonMainProcessResources",
                    ":library:metadataLinuxMainClasses",
                    ":library:metadataLinuxMainProcessResources",
                    ":library:metadataNativeMainClasses",
                    ":library:metadataNativeMainProcessResources",
                    ":library:processJvmMainResources",
                    ":library:transformCommonMainDependenciesMetadata",
                    ":library:transformLinuxMainDependenciesMetadata",
                    ":library:transformNativeMainDependenciesMetadata",
                    ":library:downloadKotlinNativeDistribution",
                )
                val thisProjectTasks = setOf(
                    ":kmpPartiallyResolvedDependenciesChecker",
                    ":checkKotlinGradlePluginConfigurationErrors",
                    ":commonizeNativeDistribution",
                    ":compileCommonMainKotlinMetadata",
                    ":compileKotlinJs",
                    ":compileKotlinJvm",
                    ":compileKotlinLinuxArm64",
                    ":compileKotlinLinuxX64",
                    ":generateProjectStructureMetadata",
                    ":generateSourceIn_commonMain_0",
                    ":jsProcessResources",
                    ":jvmProcessResources",
                    ":linuxArm64ProcessResources",
                    ":linuxX64ProcessResources",
                    ":metadataCommonMainProcessResources",
                    ":metadataLinuxMainProcessResources",
                    ":metadataNativeMainProcessResources",
                    ":processJvmMainResources",
                    ":transformCommonMainDependenciesMetadata",
                    ":transformCommonTestDependenciesMetadata",
                    ":transformLinuxMainDependenciesMetadata",
                    ":transformLinuxTestDependenciesMetadata",
                    ":transformNativeMainDependenciesMetadata",
                    ":transformNativeTestDependenciesMetadata",
                    ":downloadKotlinNativeDistribution"
                )
                assertExactTasksInGraph(
                    if (localRepository != null) {
                        thisProjectTasks
                    } else {
                        libraryTasks + thisProjectTasks
                    }
                )
                val compileTasks = setOfNotNull(
                    ":compileKotlinJvm",
                    ":compileKotlinJs",
                    ":compileKotlinLinuxArm64",
                    ":compileKotlinLinuxX64",
                )
                val specificSourceSets = sourceSetNames - "commonMain"
                val outputPerTask = compileTasks.associateWith { getOutputForTask(it, logLevel = LogLevel.INFO) }
                for ((task, taskOutput) in outputPerTask) {
                    assertFalse(
                        taskOutput.contains("generatedSource_commonMain_\\d+.kt:\\d+:\\d+ Unresolved reference 'commonMain'".toRegex()),
                        "$task should be able to resolve `commonMain()`\n$taskOutput"
                    )
                    for (sourceSet in specificSourceSets) {
                        assertTrue(
                            taskOutput.contains("generatedSource_commonMain_\\d+.kt:\\d+:\\d+ Unresolved reference '$sourceSet'".toRegex()),
                            "`$sourceSet()` should be unresolvable for $task\n$taskOutput"
                        )
                    }
                }
            }
        }
    }

    @DisplayName("single-target project does not produce actual metadata artifact with the separate kmp compilation scheme")
    @GradleTest
    fun singleTargetMetadataSeparate(gradleVersion: GradleVersion, @TempDir localRepoDir: Path) {
        doTestSingleTargetMetadata(gradleVersion, localRepoDir, true)
    }

    @DisplayName("single-target project does not produce actual metadata artifact with the current kmp compilation scheme")
    @GradleTest
    fun singleTargetMetadataCurrent(gradleVersion: GradleVersion, @TempDir localRepoDir: Path) {
        doTestSingleTargetMetadata(gradleVersion, localRepoDir, false)
    }

    @DisplayName("KT-79073 - test compilation compiles with use of internals from main code")
    @GradleTest
    fun `KT-79073 - friend fragment dependencies`(gradleVersion: GradleVersion) {
        defaultProject(
            gradleVersion = gradleVersion,
            sourceStubs = false,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjects(),
            additionalProjectConfiguration = {
                project.applyMultiplatform {
                    jvm()
                    linuxX64()
                    sourceSets.commonMain.get().compileSource("internal fun commonMain() = 42")
                    // commonTest sees commonMain's internal
                    sourceSets.commonTest.get().compileSource("fun commonTest() = commonMain()")
                    sourceSets.jvmMain.get().compileSource("internal fun jvmMain() = commonMain()")
                    // jvmTest should see main and test, also internals
                    sourceSets.jvmTest.get().compileSource("internal fun jvmTest() { commonMain(); commonTest(); jvmMain(); }")
                }
            }) {
            build(":compileTestKotlinJvm")
        }
    }

    private fun doTestSingleTargetMetadata(gradleVersion: GradleVersion, localRepoDir: Path, enableSeparateCompilation: Boolean) {
        project("empty", gradleVersion, localRepoDir = localRepoDir) {
            plugins {
                kotlin("multiplatform")
            }
            val localRepoPath = localRepoDir.absolutePathString()
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        jvm()
                        with(sourceSets) {
                            commonMain.get().compileStubSourceWithSourceSetName()
                            jvmMain.get().compileStubSourceWithSourceSetName()
                        }
                    }

                    project.setupMavenPublication(
                        "Stub",
                        PublisherConfiguration(PUBLISHED_DEP_GROUP, PUBLISHED_DEP_VERSION, localRepoPath)
                    )
                }
            }

            if (enableSeparateCompilation) {
                enableSeparateCompilation()
                buildAndFail(":publish") {
                    // Currently, it's not clear what shared source sets of single-target projects or bamboo structures should be.
                    // This test just fixates it's currently disallowed to have code in such shared source sets
                    // until we decide what exactly should be allowed there
                    assertTasksFailed(":compileKotlinJvm")
                }
            } else {
                build(":publish") {
                    val metadataJar = localRepoDir.resolveRepoArtifactPath(PUBLISHED_DEP_GROUP, projectName, PUBLISHED_DEP_VERSION)
                    assertFileExists(metadataJar)
                    ZipFile(metadataJar.toFile()).use { zip ->
                        val topLevelEntries = zip.entries().asSequence()
                            .filter { entry ->
                                val path = entry.name
                                // top-level directories contain a slash at the end of the name
                                '/' !in path || path.substring(path.indexOf('/') + 1).isEmpty()
                            }
                            .toList()

                        assert(topLevelEntries.size == 1 && topLevelEntries.none { it.name != "META-INF/" }) {
                            "Metadata JAR $metadataJar is expected to be an empty jar with META-INF only single-target KMP project. Top-level entries of the metadata jar: $topLevelEntries"
                        }
                    }
                }
            }
        }
    }

    private fun GradleProject.enableSeparateCompilation() {
        gradleProperties.appendText(
            """
                |${PropertiesProvider.PropertyNames.KOTLIN_KMP_SEPARATE_COMPILATION}=true
                |
                """.trimMargin()
        )
    }

    @DisplayName("Enabling new KMP compilation scheme should emit event KOTLIN_SEPARATE_KMP_COMPILATION_ENABLED")
    @GradleTest
    fun fusEvent(gradleVersion: GradleVersion) {
        defaultProject(
            gradleVersion,
            autoEnableSeparateKmpCompilation = false,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED),
        ) {
            val eventPrefix = "${BooleanMetrics.KOTLIN_SEPARATE_KMP_COMPILATION_ENABLED.name}="
            assertEquals(
                0,
                collectFusEvents(":compileKotlinJs", ":compileKotlinJvm", ":compileKotlinLinuxX64").count {
                    it.startsWith(eventPrefix)
                })
            enableSeparateCompilation()
            // the event is global, so check each task emits it
            assertEquals(
                1,
                collectFusEvents(*rerunTask(":compileKotlinJvm")).count {
                    it.startsWith(eventPrefix)
                })
            assertEquals(
                1,
                collectFusEvents(*rerunTask(":compileKotlinLinuxX64")).count {
                    it.startsWith(eventPrefix)
                })
            assertEquals(
                1,
                collectFusEvents(*rerunTask(":compileKotlinJs")).count {
                    it.startsWith(eventPrefix)
                })
        }
    }

    private fun defaultProject(
        gradleVersion: GradleVersion,
        additionalProjectConfiguration: Project.() -> Unit = {},
        autoEnableSeparateKmpCompilation: Boolean = true,
        buildOptions: BuildOptions = defaultBuildOptions,
        sourceStubs: Boolean = true,
        test: TestProject.() -> Unit,
    ): GradleProject = project("empty", gradleVersion, buildOptions = buildOptions) {
        plugins {
            kotlin("multiplatform")
        }
        if (autoEnableSeparateKmpCompilation) {
            enableSeparateCompilation()
        }
        buildScriptInjection {
            with(project) {
                applyMultiplatform {
                    jvm()
                    js()
                    linuxX64()
                    linuxArm64()
                    macosArm64()
                    if (sourceStubs) {
                        with(sourceSets) {
                            commonMain.get().compileStubSourceWithSourceSetName()
                        }
                    }
                }
                additionalProjectConfiguration(this)
            }
        }
        test()
    }

    companion object {
        private const val PUBLISHED_DEP_GROUP = "org.example"
        private const val PUBLISHED_DEP_VERSION = "1.0"
    }
}
