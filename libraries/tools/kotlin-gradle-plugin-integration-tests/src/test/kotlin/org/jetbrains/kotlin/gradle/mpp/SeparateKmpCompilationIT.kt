/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.*
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.gradle.util.resolveRepoArtifactPath
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@MppGradlePluginTests
@DisplayName("Separate KMP compilation a.k.a. the new KMP compilation scheme: KT-77546")
class SeparateKmpCompilationIT : KGPBaseTest() {
    @DisplayName("fragment dependencies are configured for shared source sets in a single-target project")
    @GradleTest
    fun fragmentDependenciesInSingleTargetProject(gradleVersion: GradleVersion) {
        doTestFragmentDependenciesArg(
            gradleVersion,
            targetsToInclude = listOf("jvm"),
            targetsToRun = listOf("jvm"),
            additionalProjectConfiguration = {
                val commonDependency = project.file("common-dependency.jar")
                val jvmDependency = project.file("jvm-dependency.jar")

                applyMultiplatform {
                    sourceSets.commonMain {
                        dependencies {
                            implementation(project.files(commonDependency))
                        }
                    }
                    sourceSets.jvmMain {
                        dependencies {
                            implementation(project.files(jvmDependency))
                        }
                    }
                }
            },
            assertions = { fragmentDependencies ->
                assertEquals(
                    listOf(
                        "/build/kotlinTransformedMetadataLibraries/commonMain/org.jetbrains.kotlin-kotlin-stdlib-<version>-commonMain-<klib-hash>.klib",
                        "/common-dependency.jar",
                    ).prettyPrinted,
                    fragmentDependencies.getValue("commonMain").prettyPrinted,
                    "Only common dependencies are present"
                )
            }
        )
    }

    @DisplayName("fragment dependencies are not duplicated if they are defined higher in the hierarchy")
    @GradleTest
    fun fragmentDependenciesAreDeduplicated(gradleVersion: GradleVersion) {
        doTestFragmentDependenciesArg(gradleVersion, additionalProjectConfiguration = {
            applyMultiplatform {
                sourceSets {
                    val jvmAndJs = it.create("jvmAndJs").apply {
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
            for ([_, dependencies] in fragmentDependencies) {
                for (dependency in dependencies) {
                    assertTrue(
                        visitedDependencies.add(dependency),
                        "Duplicate dependency '$dependency' found in fragment dependencies: $fragmentDependencies",
                    )
                }
            }
        }
    }

    @DisplayName("native stdlib and platform dependencies are added to fragment dependencies")
    @GradleTest
    fun nativeStdlibIsAdded(gradleVersion: GradleVersion) {
        doTestFragmentDependenciesArg(gradleVersion, targetsToRun = listOf("linuxX64")) { fragmentDependenciesPerFragment ->
            assertEquals(
                listOf(
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/common/stdlib",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64, macos_arm64)/org.jetbrains.kotlin.native.platform.builtin",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64, macos_arm64)/org.jetbrains.kotlin.native.platform.iconv",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64, macos_arm64)/org.jetbrains.kotlin.native.platform.posix",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64, macos_arm64)/org.jetbrains.kotlin.native.platform.zlib",
                ).prettyPrinted,
                fragmentDependenciesPerFragment.getValue("nativeMain").prettyPrinted,
                "nativeMain dependencies are expected contain only commonized platform libraries and stdlib",
            )

            assertEquals(
                listOf(
                    "/build/kotlinTransformedMetadataLibraries/commonMain/org.jetbrains.kotlin-kotlin-stdlib-<version>-commonMain-<klib-hash>.klib",
                ).prettyPrinted,
                fragmentDependenciesPerFragment.getValue("commonMain").prettyPrinted,
                "commonMain dependencies is expected to contain stdlib",
            )

            assertEquals(
                listOf(
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.builtin",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.iconv",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.linux",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.posix",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.zlib",
                ).prettyPrinted,
                fragmentDependenciesPerFragment.getValue("linuxMain").prettyPrinted,
                "Expected linuxMain to contain only commonized platform libraries",
            )
        }
    }

    @DisplayName("KT-82367 - native platform dependencies are added to test compilation fragment dependencies")
    @GradleTest
    fun `KT-82367 - nativePlatformDepsAreAddedToTestCompilationFragmentDependencies`(gradleVersion: GradleVersion) {
        doTestFragmentDependenciesArg(
            gradleVersion,
            targetsToRun = listOf("linuxX64"),
            compilationName = "test",
            additionalProjectPostConfiguration = {
                kotlinSourcesDir("nativeTest").createDirectories()
                    .resolve("PosixTest.kt")
                    .writeText(
                        """
                        import platform.posix.sched_yield
                        fun yieldThread() { sched_yield() }
                        """.trimIndent()
                    )
            },
        ) { fragmentDependenciesPerFragment ->
            assertEquals(
                listOf(
                    "/build/kotlinTransformedMetadataLibraries/commonTest/org.jetbrains.kotlin-kotlin-stdlib-<version>-commonMain-<klib-hash>.klib"
                ).prettyPrinted,
                fragmentDependenciesPerFragment.getValue("commonTest").prettyPrinted,
                "Only Kotlin stdlib is expected to be in 'commonTest' fragment dependencies"
            )

            assertEquals(
                listOf(
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/common/stdlib",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64, macos_arm64)/org.jetbrains.kotlin.native.platform.builtin",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64, macos_arm64)/org.jetbrains.kotlin.native.platform.iconv",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64, macos_arm64)/org.jetbrains.kotlin.native.platform.posix",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64, macos_arm64)/org.jetbrains.kotlin.native.platform.zlib",
                ).prettyPrinted,
                fragmentDependenciesPerFragment.getValue("nativeTest").prettyPrinted,
                "Only one Kotlin stdlib and only commonized dependencies are expected to be in 'nativeTest' fragment dependencies"
            )

            assertEquals(
                listOf(
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.builtin",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.iconv",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.linux",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.posix",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/commonized/<version>/(linux_arm64, linux_x64)/org.jetbrains.kotlin.native.platform.zlib",
                ).prettyPrinted,
                fragmentDependenciesPerFragment.getValue("linuxTest").prettyPrinted,
                "Only commonized dependencies are expected to be in 'linuxTest' fragment dependencies"
            )
        }
    }

    private fun doTestFragmentDependenciesArg(
        gradleVersion: GradleVersion,
        targetsToInclude: List<String> = ALL_TARGETS,
        targetsToRun: List<String> = listOf("linuxX64", "jvm", "js"),
        compilationName: String = "main",
        additionalProjectPostConfiguration: TestProject.() -> Unit = {},
        additionalProjectConfiguration: Project.() -> Unit = {},
        assertions: (Map<String, List<String>>) -> Unit,
    ) {
        defaultProject(gradleVersion, targetsToInclude, additionalProjectConfiguration) {
            additionalProjectPostConfiguration()

            @Suppress("DEPRECATION")
            val compileArgs: List<Pair<String, CommonCompilerArguments>> = providerBuildScriptReturn {
                val targets = targetsToRun.map { kotlinMultiplatform.targets.getByName(it) }
                project.provider {
                    targets.map { target ->
                        val task = target.compilations.getByName(compilationName).compileTaskProvider.get()
                        project.ignoreAccessViolations {
                            task as KotlinCompilerArgumentsProducer
                            target.name to task.createCompilerArguments() as CommonCompilerArguments
                        }
                    }
                }
            }.buildAndReturn(
                *targetsToRun.map { targetName ->
                    if (compilationName == "main") ":compileKotlin${targetName.capitalize()}"
                    else ":compile${compilationName.capitalize()}Kotlin${targetName.capitalize()}"
                }.toTypedArray(),
                deriveBuildOptions = {
                    // otherwise we would access GMT task outputs before the task execution
                    buildOptions.copy(
                        configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
                        isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
                    )
                },
            )
            for ([_, particularCompileArgs] in compileArgs) {
                val fragmentDependencies = particularCompileArgs.fragmentDependencies
                val dependenciesPerFragment = fragmentDependencies
                    .groupBy({ it.substringBefore(":") }) { it.substringAfter(":") }
                    .mapValues {
                        it.value
                            .sanitizeDependencies(
                                projectPath,
                                buildOptions.kotlinVersion,
                                konanDir,
                            )
                            .sorted()
                    }
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
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899().copy(separateCompilation = true),
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
                buildOptions = buildOptions.copy(continueAfterFailure = true, separateCompilation = true)
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
                    ":library:transformWebMainDependenciesMetadata",
                    ":library:transformLinuxMainDependenciesMetadata",
                    ":library:transformLinuxMainCInteropDependenciesMetadata",
                    ":library:transformNativeMainDependenciesMetadata",
                    ":library:transformNativeMainCInteropDependenciesMetadata",
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
                    ":transformWebMainDependenciesMetadata",
                    ":transformLinuxMainDependenciesMetadata",
                    ":transformLinuxMainCInteropDependenciesMetadata",
                    ":transformLinuxTestDependenciesMetadata",
                    ":transformLinuxTestCInteropDependenciesMetadata",
                    ":transformNativeMainDependenciesMetadata",
                    ":transformNativeMainCInteropDependenciesMetadata",
                    ":transformNativeTestDependenciesMetadata",
                    ":transformNativeTestCInteropDependenciesMetadata",
                    ":downloadKotlinNativeDistribution",
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
                for ([task, taskOutput] in outputPerTask) {
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
        doTestSingleTargetMetadata(gradleVersion, localRepoDir, enableSeparateCompilation = true)
    }

    @DisplayName("single-target project does not produce actual metadata artifact with the current kmp compilation scheme")
    @GradleTest
    fun singleTargetMetadataCurrent(gradleVersion: GradleVersion, @TempDir localRepoDir: Path) {
        doTestSingleTargetMetadata(gradleVersion, localRepoDir, enableSeparateCompilation = false)
    }

    // Generally should be covered by other tests once KMP separate compilation is enabled by default
    @DisplayName("single-target native project compiles successfully")
    @GradleTest
    fun singleTargetNativeProject(gradleVersion: GradleVersion) {
        doTestFragmentDependenciesArg(
            gradleVersion = gradleVersion,
            targetsToInclude = listOf("linuxX64"),
            targetsToRun = listOf("linuxX64"),
            compilationName = "test"
        ) { fragmentDependenciesPerFragment ->
            assertEquals(
                listOf(
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/common/stdlib",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.builtin",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.iconv",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.linux",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.posix",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.zlib",
                ).prettyPrinted,
                fragmentDependenciesPerFragment.getValue("commonTest").prettyPrinted,
                "Kotlin stdlib and platform dependencies are expected to be in 'commonTest' fragment dependencies"
            )

            assertEquals(
                listOf(
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/common/stdlib",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.builtin",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.iconv",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.linux",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.posix",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.zlib",
                ).prettyPrinted,
                fragmentDependenciesPerFragment.getValue("nativeTest").prettyPrinted,
                "Only one Kotlin stdlib and platform dependencies are expected to be in 'nativeTest' fragment dependencies"
            )

            assertEquals(
                listOf(
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/common/stdlib",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.builtin",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.iconv",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.linux",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.posix",
                    "<distribution>/kotlin-native-prebuilt-<prebuilt-version>/klib/platform/linux_x64/org.jetbrains.kotlin.native.platform.zlib",
                ).prettyPrinted,
                fragmentDependenciesPerFragment.getValue("linuxTest").prettyPrinted,
                "Only platform dependencies are expected to be in 'linuxTest' fragment dependencies"
            )
        }
    }

    @DisplayName("KT-79073 - test compilation compiles with use of internals from main code")
    @GradleTest
    fun `KT-79073 - friend fragment dependencies`(gradleVersion: GradleVersion) {
        defaultProject(
            gradleVersion = gradleVersion,
            sourceStubs = false,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
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
        project(
            "empty",
            gradleVersion,
            localRepoDir = localRepoDir,
            buildOptions = defaultBuildOptions.copy(separateCompilation = enableSeparateCompilation),
        ) {
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

    @DisplayName("Enabling new KMP compilation scheme should emit event KOTLIN_SEPARATE_KMP_COMPILATION_ENABLED")
    @GradleTest
    fun fusEvent(gradleVersion: GradleVersion) {
        defaultProject(
            gradleVersion,
            autoEnableSeparateKmpCompilation = false,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {
            val eventPrefix = "${BooleanMetrics.KOTLIN_SEPARATE_KMP_COMPILATION_ENABLED.name}="
            assertEquals(
                0,
                collectFusEvents(":compileKotlinJs", ":compileKotlinJvm", ":compileKotlinLinuxX64").count {
                    it.startsWith(eventPrefix)
                })
            val separateCompilationOptions: TestProject.() -> BuildOptions = { buildOptions.copy(separateCompilation = true) }
            // the event is global, so check each task emits it
            assertEquals(
                1,
                collectFusEvents(*rerunTask(":compileKotlinJvm"), deriveBuildOptions = separateCompilationOptions).count {
                    it.startsWith(eventPrefix)
                })
            assertEquals(
                1,
                collectFusEvents(*rerunTask(":compileKotlinLinuxX64"), deriveBuildOptions = separateCompilationOptions).count {
                    it.startsWith(eventPrefix)
                })
            assertEquals(
                1,
                collectFusEvents(*rerunTask(":compileKotlinJs"), deriveBuildOptions = separateCompilationOptions).count {
                    it.startsWith(eventPrefix)
                })
        }
    }

    @DisplayName("Fragment dependencies should be propagated to non-default common source sets (single Linux target)")
    @GradleTest
    fun nonDefaultHierarchySingleLinux(gradleVersion: GradleVersion) {
        testNonDefaultHierarchySingleLinux(gradleVersion, includeLinuxArm64 = false)
    }

    @DisplayName("Fragment dependencies should be propagated to non-default common source sets (multiple Linux targets)")
    @GradleTest
    fun nonDefaultHierarchyMultipleLinux(gradleVersion: GradleVersion) {
        testNonDefaultHierarchySingleLinux(gradleVersion, includeLinuxArm64 = true)
    }

    private fun testNonDefaultHierarchySingleLinux(gradleVersion: GradleVersion, includeLinuxArm64: Boolean) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        @OptIn(ExperimentalKotlinGradlePluginApi::class)
                        applyDefaultHierarchyTemplate {
                            common {
                                group("commonKotlin") {
                                    group("native") {
                                        group("darwin") {
                                            withLinuxX64()
                                            if (includeLinuxArm64) {
                                                withLinuxArm64()
                                            }
                                        }
                                        group("windows") {
                                            withMingw()
                                        }
                                    }
                                }
                            }
                        }

                        mingwX64()
                        linuxX64()
                        if (includeLinuxArm64) {
                            linuxArm64()
                        }
                    }
                }
            }
            kotlinSourcesDir("darwinMain").source("darwinMain.kt") {
                """
                    import platform.linux.ERROR

                    val theDate: Int = ERROR
                """.trimIndent()
            }


            build(
                "compileKotlinLinuxX64",
                buildOptions = defaultBuildOptions.copy(separateCompilation = true)
            )
        }
    }

    private fun defaultProject(
        gradleVersion: GradleVersion,
        targetsToInclude: List<String> = ALL_TARGETS,
        additionalProjectConfiguration: Project.() -> Unit = {},
        autoEnableSeparateKmpCompilation: Boolean = true,
        buildOptions: BuildOptions = defaultBuildOptions,
        sourceStubs: Boolean = true,
        test: TestProject.() -> Unit,
    ): GradleProject = project(
        "empty",
        gradleVersion,
        buildOptions = if (autoEnableSeparateKmpCompilation) buildOptions.copy(separateCompilation = true) else buildOptions,
    ) {
        plugins {
            kotlin("multiplatform")
        }
        buildScriptInjection {
            with(project) {
                applyMultiplatform {
                    if ("jvm" in targetsToInclude) {
                        jvm()
                    }
                    if ("js" in targetsToInclude) {
                        js()
                    }
                    if ("linuxX64" in targetsToInclude) {
                        linuxX64()
                    }
                    if ("linuxArm64" in targetsToInclude) {
                        linuxArm64()
                    }
                    if ("macosArm64" in targetsToInclude) {
                        macosArm64()
                    }
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

    private val nativePrebuiltVersionRegex = """kotlin-native-prebuilt-[^/]+""".toRegex()

    /**
     * See 'CompositeMetadataArtifactImpl.ArtifactFile.checksum' logic.
     */
    private val transformedMetadataLibraryChecksumRegex =
        """(kotlinTransformedMetadataLibraries/.*-)[A-Za-z0-9_-]{5}[AQgw](\.klib)""".toRegex()

    private fun List<String>.sanitizeDependencies(
        projectPath: Path,
        version: String,
        distributionPath: Path,
    ): List<String> = map {
        it.substringAfter(projectPath.absolutePathString())
            .replace(distributionPath.absolutePathString(), "<distribution>")
            .normalizePath()
            .replace(nativePrebuiltVersionRegex, "kotlin-native-prebuilt-<prebuilt-version>")
            .replace(version, "<version>")
            .replace(transformedMetadataLibraryChecksumRegex, "$1<klib-hash>$2")
    }

    companion object {
        private const val PUBLISHED_DEP_GROUP = "org.example"
        private const val PUBLISHED_DEP_VERSION = "1.0"
        private val ALL_TARGETS: List<String> = listOf("jvm", "js", "linuxX64", "linuxArm64", "macosArm64")
    }
}
