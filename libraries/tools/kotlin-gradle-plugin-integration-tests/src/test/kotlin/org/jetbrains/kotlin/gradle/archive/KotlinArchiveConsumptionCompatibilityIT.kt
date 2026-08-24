/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.archive

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.addKgpToBuildScriptCompilationClasspath
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.buildScriptReturn
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.gradle.testing.ResolvedComponentWithArtifacts
import org.jetbrains.kotlin.gradle.testing.compilationResolution
import org.jetbrains.kotlin.gradle.testing.runtimeResolution
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.ignoreAccessViolations
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

@DisplayName("Consumption of a project published in the Kotlin Archive format by the previous Kotlin release")
@MppGradlePluginTests
class KotlinArchiveConsumptionCompatibilityIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun jvmProjectConsumptionTest(gradleVersion: GradleVersion) {
        val publishedProject = publishKotlinArchive(gradleVersion)

        compatibleKotlinConsumer(gradleVersion, publishedProject) {
            addSourceFile("main", consumerCallingJvmDeclarations)
            buildScriptInjection {
                project.applyJvm { }
                project.dependencies.add("implementation", publishedProject.rootCoordinate)
            }
        }.build("compileKotlin") {
            assertTasksExecuted(":compileKotlin")
        }
    }

    @GradleTest
    fun jvmOnlyMultiplatformProjectConsumptionTest(gradleVersion: GradleVersion) {
        val publishedProject = publishKotlinArchive(gradleVersion)

        compatibleKotlinConsumer(gradleVersion, publishedProject) {
            addSourceFile("jvmMain", consumerCallingJvmDeclarations)
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()

                    sourceSets.commonMain.dependencies {
                        implementation(publishedProject.rootCoordinate)
                    }
                }
            }
        }.build("compileKotlinJvm") {
            assertTasksExecuted(":compileKotlinJvm")
        }
    }

    @GradleTest
    fun otherTargetsConsumptionReportsUnsupportedArchiveTest(gradleVersion: GradleVersion) {
        val publishedProject = publishKotlinArchive(gradleVersion)

        val consumer = compatibleKotlinConsumer(gradleVersion, publishedProject) {
            addSourceFile("commonMain", "fun consume() {\n    commonMain()\n}")
            buildScriptInjection {
                project.applyMultiplatform {
                    js()
                    wasmJs()
                    linuxX64()

                    sourceSets.commonMain.dependencies {
                        implementation(publishedProject.rootCoordinate)
                    }
                }
            }
        }

        unsupportedCompilationTasks.forEach { compilationTask ->
            consumer.buildAndFail(compilationTask) {
                assertTasksFailed(compilationTask)
                // The severity can't be checked here: this diagnostic is reported with an id suffix,
                // and the assertion matches the id and the severity as a single string
                assertHasDiagnostic(KotlinToolingDiagnostics.UnsupportedKotlinArchiveUsage)
            }
        }
    }

    @GradleAndroidTest
    @AndroidGradlePluginTests
    @AndroidTestVersions(minVersion = TestVersions.AGP.MAX_SUPPORTED)
    fun jvmAndAndroidProjectConsumptionTest(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val publishedProject = publishKotlinArchive(gradleVersion)

        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                kotlinVersion = TESTED_KOTLIN_VERSION,
                androidVersion = androidVersion,
                // `com.android.kotlin.multiplatform.library` requires the new AGP DSL
                enableLegacyAgpDsl = false,
            ),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            addPublishedProjectToRepositories(publishedProject)
            settingsBuildScriptInjection {
                settings.rootProject.name = "consumer"
            }
            addSourceFile("jvmMain", consumerCallingJvmDeclarations)
            addSourceFile("androidMain", consumerCallingJvmDeclarations)
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    jvm()
                    targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach { target ->
                        target.compileSdk = 34
                        target.namespace = "com.example.consumer"
                    }

                    sourceSets.commonMain.dependencies {
                        implementation(publishedProject.rootCoordinate)
                    }
                }
            }
        }.build(":compileKotlinJvm", ":compileAndroidMain") {
            assertTasksExecuted(":compileKotlinJvm", ":compileAndroidMain")
        }
    }

    @GradleTest
    fun legacyPlatformDependencyIsReplacedByArchiveTest(gradleVersion: GradleVersion) {
        val library = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            settingsBuildScriptInjection {
                settings.rootProject.name = "library"
            }
            addSourceFile("commonMain", "fun libraryCommon() {}")
            addSourceFile("jsMain", "fun libraryJs() {}")
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    js()
                }
            }
        }
        val legacyLibrary = library.publish(publisherConfiguration = libraryPublisherConfiguration("1.0"))
        val archiveLibrary = library.publish(
            ENABLE_KAR_PUBLICATION,
            publisherConfiguration = libraryPublisherConfiguration("2.0"),
        )

        val intermediateLibrary = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(legacyLibrary)
            settingsBuildScriptInjection {
                settings.rootProject.name = "intermediateLibrary"
            }
            addSourceFile("jsMain", "fun intermediateJs() {\n    libraryJs()\n}")
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    js()

                    // That's quite unconventional way to depend, but people to it sometimes
                    // It causes a problem - in KAR there is no more `library-js`
                    sourceSets.jsMain.dependencies {
                        api("${TEST_GROUP}:library-js:1.0")
                    }
                }
            }
        }.publish(
            publisherConfiguration = PublisherConfiguration(group = TEST_GROUP, version = "1.0"),
        )

        val app = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(archiveLibrary)
            addPublishedProjectToRepositories(intermediateLibrary)
            settingsBuildScriptInjection {
                settings.rootProject.name = "app"
            }
            addSourceFile("jsMain", "fun appJs() {\n    libraryCommon()\n    libraryJs()\n    intermediateJs()\n}")
            buildScriptInjection {
                project.applyMultiplatform {
                    this.jvm()
                    this.js()

                    sourceSets.commonMain.dependencies {
                        implementation(intermediateLibrary.rootCoordinate)
                        implementation(archiveLibrary.rootCoordinate)
                    }
                }
            }
        }

        app.build(":compileKotlinJs") {
            assertTasksExecuted(":compileKotlinJs")
        }

        val jsAppResolutionResult = app.buildScriptReturn {
            project.ignoreAccessViolations {
                val js = kotlinMultiplatform.targets.getByName("js")
                mapOf(
                    "compile" to js.compilationResolution(),
                    "runtime" to js.runtimeResolution(),
                )
            }
        }.buildAndReturn(":compileKotlinJs")
        assertEquals(
            expectedAppJsResolution.prettyPrinted,
            jsAppResolutionResult.prettyPrinted,
        )
    }

    private fun libraryPublisherConfiguration(version: String) = PublisherConfiguration(
        group = TEST_GROUP,
        version = version,
    )

    /**
     * The archive is published by the Kotlin version under test, and consumed by [TESTED_KOTLIN_VERSION].
     */
    private fun publishKotlinArchive(gradleVersion: GradleVersion): PublishedProject =
        kotlinArchiveProducer(gradleVersion).publish()

    private fun compatibleKotlinConsumer(
        gradleVersion: GradleVersion,
        publishedProject: PublishedProject,
        configure: TestProject.() -> Unit,
    ): TestProject = project(
        "empty",
        gradleVersion,
        buildOptions = defaultBuildOptions.copy(kotlinVersion = TESTED_KOTLIN_VERSION),
    ) {
        addKgpToBuildScriptCompilationClasspath()
        addPublishedProjectToRepositories(publishedProject)
        settingsBuildScriptInjection {
            settings.rootProject.name = "consumer"
        }
        configure()
    }

    companion object {
        // TODO: change to 2.4.20, once it is released
        private const val TESTED_KOTLIN_VERSION = "2.4.20-RC"

        private const val ENABLE_KAR_PUBLICATION = "-Pkotlin.publicationFormat=KOTLIN_ARCHIVE"

        // Note: the group can't contain dots, as [PublishedProject] resolves it as a single directory
        private const val TEST_GROUP = "kotlinArchiveTest"

        private val expectedAppJsResolution = mapOf(
            "compile" to mapOf(
                "kotlinArchiveTest:intermediateLibrary-js:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                        mapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.js.compiler" to "ir",
                            "org.jetbrains.kotlin.platform.type" to "js",
                        ),
                    ),
                    configuration = "jsApiElements-published",
                ),
                "kotlinArchiveTest:intermediateLibrary:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                    ),
                    configuration = "jsApiElements-published",
                ),
                "kotlinArchiveTest:library:2.0" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                        mapOf(
                            "artifactType" to "xz",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-api",
                            "org.jetbrains.kotlin.js.compiler" to "ir",
                            "org.jetbrains.kotlin.kar.compression.method" to "NONE",
                            "org.jetbrains.kotlin.kar.state" to "PLATFORM_ARTIFACTS_EXTRACTED",
                            "org.jetbrains.kotlin.platform.type" to "js",
                        ),
                    ),
                    configuration = "jsApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-dom-api-compat:$KOTLIN_VERSION" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                        mapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.js.compiler" to "ir",
                            "org.jetbrains.kotlin.platform.type" to "js",
                        ),
                    ),
                    configuration = "jsApiElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib-js:$KOTLIN_VERSION" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                        mapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-api",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.js.compiler" to "ir",
                            "org.jetbrains.kotlin.klib.packaging" to "packed",
                            "org.jetbrains.kotlin.platform.type" to "js",
                        ),
                    ),
                    configuration = "jsApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                    ),
                    configuration = "jsApiElements",
                ),
            ),
            "runtime" to mapOf(
                "kotlinArchiveTest:intermediateLibrary-js:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                        mapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-runtime",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.js.compiler" to "ir",
                            "org.jetbrains.kotlin.platform.type" to "js",
                        ),
                    ),
                    configuration = "jsRuntimeElements-published",
                ),
                "kotlinArchiveTest:intermediateLibrary:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                    ),
                    configuration = "jsRuntimeElements-published",
                ),
                "kotlinArchiveTest:library:2.0" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                        mapOf(
                            "artifactType" to "xz",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-runtime",
                            "org.jetbrains.kotlin.js.compiler" to "ir",
                            "org.jetbrains.kotlin.kar.compression.method" to "NONE",
                            "org.jetbrains.kotlin.kar.state" to "PLATFORM_ARTIFACTS_EXTRACTED",
                            "org.jetbrains.kotlin.platform.type" to "js",
                        ),
                    ),
                    configuration = "jsRuntimeElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-dom-api-compat:$KOTLIN_VERSION" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                        mapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-runtime",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.js.compiler" to "ir",
                            "org.jetbrains.kotlin.platform.type" to "js",
                        ),
                    ),
                    configuration = "jsRuntimeElements-published",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib-js:$KOTLIN_VERSION" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                        mapOf(
                            "artifactType" to "klib",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "non-jvm",
                            "org.gradle.usage" to "kotlin-runtime",
                            "org.jetbrains.kotlin.cinteropCommonizerArtifactType" to "klib",
                            "org.jetbrains.kotlin.js.compiler" to "ir",
                            "org.jetbrains.kotlin.klib.packaging" to "packed",
                            "org.jetbrains.kotlin.platform.type" to "js",
                        ),
                    ),
                    configuration = "jsRuntimeElements",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION" to ResolvedComponentWithArtifacts(
                    artifacts = listOf(
                    ),
                    configuration = "jsRuntimeElements",
                ),
            ),
        )

        private val consumerCallingJvmDeclarations = "fun consume() {\n    commonMain()\n    jvmMain()\n}"

        private val unsupportedCompilationTasks = listOf(
            ":compileKotlinJs",
            ":compileKotlinWasmJs",
            ":compileKotlinLinuxX64",
        )
    }
}
