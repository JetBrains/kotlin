/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.uklibs

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.*
import org.jetbrains.kotlin.gradle.testing.PrettyPrint
import org.jetbrains.kotlin.gradle.testing.ResolvedComponentWithArtifacts
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies
import org.jetbrains.kotlin.gradle.utils.named
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.io.Serializable
import kotlin.String
import kotlin.collections.Map
import kotlin.io.path.pathString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalWasmDsl::class)
@MppGradlePluginTests
@DisplayName("Smoke test uklib consumption")
class UklibConsumptionIT : KGPBaseTest() {

    @GradleAndroidTest
    fun `uklib consumption smoke - in kotlin compilations of a symmetric consumer and producer projects - with all metadata compilations`(
        gradleVersion: GradleVersion,
        androidVersion: String,
    ) {
        val symmetricTargets: KotlinMultiplatformExtension.() -> Unit = {
            @Suppress("DEPRECATION")
            androidTarget().publishLibraryVariants("debug", "release")
            linuxArm64()
            iosArm64()
            iosX64()
            macosArm64()
            jvm()
            js()
            wasmJs()
            wasmWasi()
        }
        val publisher = publishUklib(
            gradleVersion,
            androidVersion = androidVersion
        ) {
            project.plugins.apply("com.android.library")
            with(project.extensions.getByType(LibraryExtension::class.java)) {
                compileSdk = 23
                namespace = "kotlin.producer"
            }

            jvmToolchain(8)

            symmetricTargets()
            sourceSets.all {
                it.compileSource(
                    "object Producer_${it.name}"
                )
            }
        }

        val producerConsumerVisibility = mapOf(
            "common" to listOf(
                "Producer_commonMain",
            ),
            "native" to listOf(
                "Producer_commonMain",
                "Producer_nativeMain",
            ),
            "apple" to listOf(
                "Producer_commonMain",
                "Producer_nativeMain",
                "Producer_appleMain",
            ),
            "macos" to listOf(
                "Producer_commonMain",
                "Producer_nativeMain",
                "Producer_appleMain",
                "Producer_macosMain"
            ),
            "ios" to listOf(
                "Producer_commonMain",
                "Producer_nativeMain",
                "Producer_appleMain",
                "Producer_iosMain"
            ),
            "linux" to listOf(
                "Producer_commonMain",
                "Producer_nativeMain",
                "Producer_linuxMain",
            ),
            "jvm" to listOf(
                "Producer_commonMain",
                "Producer_jvmMain",
            ),
            "js" to listOf(
                "Producer_commonMain",
                "Producer_jsMain",
            ),
            "wasmJs" to listOf(
                "Producer_commonMain",
                "Producer_wasmJsMain",
            ),
            "wasmWasi" to listOf(
                "Producer_commonMain",
                "Producer_wasmWasiMain"
            ),
            "iosArm64" to listOf(
                "Producer_commonMain",
                "Producer_nativeMain",
                "Producer_appleMain",
                "Producer_iosArm64Main",
            ),
            "iosX64" to listOf(
                "Producer_commonMain",
                "Producer_nativeMain",
                "Producer_appleMain",
                "Producer_iosX64Main",
            ),
            "linuxArm64" to listOf(
                "Producer_commonMain",
                "Producer_nativeMain",
                "Producer_linuxMain",
                "Producer_linuxArm64Main",
            ),
            "macosArm64" to listOf(
                "Producer_commonMain",
                "Producer_nativeMain",
                "Producer_appleMain",
                "Producer_macosMain",
                "Producer_macosArm64Main",
            ),
            "android" to listOf(
                "Producer_commonMain",
                "Producer_androidMain",
            ),
            "web" to listOf(
                "Producer_webMain",
            ),
        ).flatMap {
            listOf(
                it.key + "Main" to it.value,
                it.key + "Test" to it.value,
            )
        }.toMap().toMutableMap()

        listOf(
            "androidDebug",
            "androidRelease",
            "androidInstrumentedTest",
            "androidInstrumentedTestDebug",
            "androidInstrumentedTestRelease",
            "androidUnitTest",
            "androidUnitTestDebug",
            "androidUnitTestRelease",
        ).forEach {
            producerConsumerVisibility[it] = producerConsumerVisibility["androidMain"]!!
        }

        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
                androidVersion = androidVersion,
            ),
        ) {
            addAgpToBuildScriptCompilationClasspath(androidVersion)
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(publisher)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.plugins.apply("com.android.library")
                with(project.extensions.getByType(LibraryExtension::class.java)) {
                    compileSdk = 23
                    namespace = "kotlin.consumer"
                }

                project.applyMultiplatform {
                    symmetricTargets()

                    jvmToolchain(8)

                    sourceSets.all {
                        val producerTypes = producerConsumerVisibility[it.name] ?: error("Missing producer declaration for ${it.name}")
                        val arguments = producerTypes.joinToString(", ") { "${it}: ${it}" }
                        it.compileSource(
                            """
                            fun consumeIn_${it.name}(${arguments}) {}
                            """.trimIndent()
                        )
                    }

                    sourceSets.commonMain.dependencies {
                        implementation(publisher.rootCoordinate)
                    }
                }
            }

            val resolvedProducerIosArm64Variant = buildScriptReturn {
                project.ignoreAccessViolations {
                    kotlinMultiplatform.iosArm64().compilationResolution()
                }
            }.buildAndReturn()

            // Make sure that we are actually resolving uklib from metadataSources configuration
            assertEquals(
                mapOf<ComponentPath, ResolvedComponentWithArtifacts>(
                    "foo:empty:1.0" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklib",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "ios_arm64",
                            ),
                        ),
                        configuration = "uklibApiElements",
                    ),
                    "org.jetbrains.kotlin:kotlin-dom-api-compat:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(),
                        configuration = "commonFakeApiElements-published",
                    ),
                    "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(),
                        configuration = "nativeApiElements",
                    ),
                ).prettyPrinted,
                resolvedProducerIosArm64Variant.prettyPrinted,
            )

            // FIXME: Run test compilations
            build("assemble")

            data class KlibsToCheck(
                val iosArm64Klib: File,
                val iosMainKlib: File,
                val commonMainKlib: File,
            ) : Serializable

            val klibs = buildScriptReturn {
                KlibsToCheck(
                    kotlinMultiplatform.iosArm64().compilations.getByName("main").compileTaskProvider.flatMap { it.outputFile }.get(),
                    kotlinMultiplatform.metadata().compilations.getByName("iosMain").output.classesDirs.singleFile,
                    kotlinMultiplatform.metadata().compilations.getByName("commonMain").output.classesDirs.singleFile,
                )
            }.buildAndReturn()

            assertEquals(
                """
                /consumeIn_appleMain|consumeIn_appleMain(Producer_commonMain;Producer_nativeMain;Producer_appleMain){}[0]
                /consumeIn_commonMain|consumeIn_commonMain(Producer_commonMain){}[0]
                /consumeIn_iosArm64Main|consumeIn_iosArm64Main(Producer_commonMain;Producer_nativeMain;Producer_appleMain;Producer_iosArm64Main){}[0]
                /consumeIn_iosMain|consumeIn_iosMain(Producer_commonMain;Producer_nativeMain;Producer_appleMain;Producer_iosMain){}[0]
                /consumeIn_nativeMain|consumeIn_nativeMain(Producer_commonMain;Producer_nativeMain){}[0]

                """.trimIndent(),
                dumpKlibMetadataSignatures(klibs.iosArm64Klib),
            )

            assertEquals(
                """
                /consumeIn_iosMain|consumeIn_iosMain(Producer_commonMain;Producer_nativeMain;Producer_appleMain;Producer_iosMain){}[0]

                """.trimIndent(),
                dumpKlibMetadataSignatures(klibs.iosMainKlib),
            )

            assertEquals(
                """
                /consumeIn_commonMain|consumeIn_commonMain(Producer_commonMain){}[0]

                """.trimIndent(),
                dumpKlibMetadataSignatures(klibs.commonMainKlib),
            )
        }
    }

    private fun publishUklib(
        gradleVersion: GradleVersion,
        androidVersion: String? = null,
        publisherConfiguration: KotlinMultiplatformExtension.() -> Unit,
    ): PublishedProject {
        return project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = androidVersion,
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
            ),
        ) {
            if (androidVersion != null) addAgpToBuildScriptCompilationClasspath(androidVersion)
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    publisherConfiguration()
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration())
    }

    @GradleTest
    fun `uklib consumption - with a subset of targets - resolves`(
        version: GradleVersion,
    ) {
        val publisher = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "dependency"))

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(publisher)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.commonMain.dependencies {
                        implementation(publisher.rootCoordinate)
                    }
                }
            }

            // FIXME: Rewrite this test to only test for resolution and nothing else
            build("assemble")
        }
    }

    @GradleTest
    fun `uklib consumption - transitive uklib is consumed through a jvm dependency`(
        version: GradleVersion,
    ) {
        val transitive = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "transitive"))

        val direct = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(transitive)
            buildScriptInjection {
                project.plugins.apply("java-library")
                java.sourceSets.all {
                    it.compileJavaSource(
                        project,
                        className = "Direct_${it.name}",
                        """
                            public class Direct_${it.name} { }
                        """.trimIndent()
                    )
                }
                project.configurations.getByName("api").dependencies.add(
                    project.dependencies.create(transitive.rootCoordinate)
                )
            }
        }.publishJava(PublisherConfiguration(group = "direct"))

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(direct)
            addPublishedProjectToRepositories(transitive)
            buildScriptInjection {
                project.computeTransformedLibraryChecksum(false)
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.all { it.compileStubSourceWithSourceSetName() }
                    sourceSets.commonMain.dependencies {
                        implementation(direct.rootCoordinate)
                    }
                }
            }

            build("assemble")

            val classpath = providerBuildScriptReturn {
                project.locateOrRegisterMetadataDependencyTransformationTask(
                    kotlinMultiplatform.sourceSets.getByName("commonMain")
                ).flatMap { it.allTransformedLibraries() }
            }.buildAndReturn(
                buildScriptReturn {
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        kotlinMultiplatform.sourceSets.getByName("commonMain")
                    ).get().name
                }.buildAndReturn()
            )

            assertEquals(
                listOf(
                    listOf("build", "kotlinTransformedMetadataLibraries", "commonMain", "uklib-transitive-empty-1.0-commonMain-"),
                ),
                classpath.filterNot {
                    "kotlin-stdlib" in it.name
                }.map {
                    it.toPath().toList().takeLast(4).map { it.pathString }
                }
            )
        }
    }

    // @GradleTest
    fun `transitive psm with jvm consumption`(
        version: GradleVersion,
    ) {
        // FIXME: Document that Maven consumers of transitive PSM dependencies will not work
    }

    @GradleAndroidTest
    fun `uklib consumption - androidTarget consumes jvm only uklib`(
        version: GradleVersion,
        agpVersion: String,
    ) {
        val direct = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    jvm()
                    sourceSets.commonMain.get().compileSource("class Jvm")
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "transitive"))

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(direct)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.plugins.apply("com.android.library")
                with(project.extensions.getByType(LibraryExtension::class.java)) {
                    compileSdk = 23
                    namespace = "kotlin.multiplatform.projects"
                }
                project.applyMultiplatform {
                    @Suppress("DEPRECATION")
                    androidTarget()
                    sourceSets.commonMain.get().compileSource("fun consume() { Jvm() }")
                    sourceSets.commonMain.dependencies {
                        implementation(direct.rootCoordinate)
                    }
                }
            }

            // FIXME: Is this actually supposed to pass?
            // FIXME: Catch specific compilation failure or reproduce the resolution error
            buildAndFail("assemble", buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion))
        }
    }

    @GradleTest
    fun `uklib consumption - java resolvable configurations can resolve uklibs`(
        version: GradleVersion,
    ) {
        val producer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    jvm()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer"))

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(producer)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    jvm()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    sourceSets.commonMain.dependencies {
                        api(producer.rootCoordinate)
                    }
                }
            }

            assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
                mutableMapOf<String, ResolvedComponentWithArtifacts>(
                    "org.jetbrains.kotlin:kotlin-stdlib:${buildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "jar",
                                "org.gradle.category" to "library",
                                "org.gradle.jvm.environment" to "standard-jvm",
                                "org.gradle.libraryelements" to "jar",
                                "org.gradle.usage" to "java-api",
                                "org.jetbrains.kotlin.platform.type" to "jvm",
                            ),
                        ),
                        configuration = "jvmApiElements",
                    ),
                    "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "jar",
                                "org.gradle.category" to "library",
                                "org.gradle.libraryelements" to "jar",
                                "org.gradle.usage" to "java-api",
                            ),
                        ),
                        configuration = "compile",
                    ),
                    "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklib",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-api",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "jvm",
                            ),
                        ),
                        configuration = "uklibApiElements",
                    ),
                ).prettyPrinted,
                buildScriptReturn {
                    project.ignoreAccessViolations {
                        project.configurations.getByName(
                            java.sourceSets.getByName("jvmMain").compileClasspathConfigurationName
                        ).resolveProjectDependencyComponentsWithArtifacts()
                    }
                }.buildAndReturn("assemble").prettyPrinted
            )
            assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
                mutableMapOf<String, ResolvedComponentWithArtifacts>(
                    "org.jetbrains.kotlin:kotlin-stdlib:${buildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "jar",
                                "org.gradle.category" to "library",
                                "org.gradle.jvm.environment" to "standard-jvm",
                                "org.gradle.libraryelements" to "jar",
                                "org.gradle.usage" to "java-runtime",
                                "org.jetbrains.kotlin.platform.type" to "jvm",
                            ),
                        ),
                        configuration = "jvmRuntimeElements",
                    ),
                    "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "jar",
                                "org.gradle.category" to "library",
                                "org.gradle.libraryelements" to "jar",
                                "org.gradle.usage" to "java-runtime",
                            ),
                        ),
                        configuration = "runtime",
                    ),
                    "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "uklib",
                                "org.gradle.category" to "library",
                                "org.gradle.usage" to "kotlin-uklib-runtime",
                                "org.jetbrains.kotlin.uklib" to "true",
                                "org.jetbrains.kotlin.uklibState" to "decompressed",
                                "org.jetbrains.kotlin.uklibView" to "jvm",
                            ),
                        ),
                        configuration = "uklibRuntimeElements",
                    ),
                ).prettyPrinted,
                buildScriptReturn {
                    project.ignoreAccessViolations {
                        project.configurations.getByName(
                            java.sourceSets.getByName("jvmMain").runtimeClasspathConfigurationName
                        ).resolveProjectDependencyComponentsWithArtifacts()
                    }
                }.buildAndReturn("assemble").prettyPrinted
            )
        }
    }

    @GradleTest
    fun `uklib consumption - linkage configurations consume uklibs`(
        version: GradleVersion,
    ) {
        val direct = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    sourceSets.commonMain.get().compileSource(
                        """
                        class Producer
                        class ExportMe
                        """.trimIndent()
                    )
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer"))

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(direct)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64 {
                        binaries.staticLib {
                            export(direct.rootCoordinate)
                        }
                    }
                    sourceSets.commonMain.get().compileSource("fun consume(producer: Producer) {}")
                    sourceSets.commonMain.get().dependencies {
                        api(direct.rootCoordinate)
                    }
                }
            }

            // FIXME: Validate properly we resolved Uklib in the export and -library configurations
            build("linkDebugStaticLinuxArm64")
        }
    }

    @GradleTest
    fun `uklib consumption - jvm binaries consume uklibs`(
        version: GradleVersion,
    ) {
        val direct = project("empty", version) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "producer"
            }
            buildScriptInjection {
                project.setUklibPublicationStrategy()
            }
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()
                    jvm()
                    sourceSets.commonMain.get().compileSource(
                        """
                        data class Producer(val value: String = "Foo")
                        """.trimIndent()
                    )
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer"))

        project("empty", version) {
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.setUklibResolutionStrategy()
            }
            plugins {
                kotlin("multiplatform")
            }
            addPublishedProjectToRepositories(direct)
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()
                    jvm {
                        binaries {
                            executable {
                                mainClass.set("Main")
                            }
                        }
                    }
                    sourceSets.commonMain.get().compileSource("""
                        object Main {
                            @JvmStatic
                            fun main(args: Array<String>) {
                                println(Producer())
                            }
                        }
                    """.trimIndent())
                    sourceSets.commonMain.get().dependencies {
                        api(direct.rootCoordinate)
                    }
                }
            }

            val runJvmClasspath: Set<File> = providerBuildScriptReturn {
                project.provider {
                    "waitForConfigurationToEnd"
                }.flatMap {
                    project.tasks.named<JavaExec>("runJvm").flatMap { task ->
                        val classpath = task.classpath
                        task.outputs.files.elements.map {
                            classpath.files
                        }
                    }
                }
            }.buildAndReturn("runJvm")
            val matchers = listOf(
                File("empty/build/classes/kotlin/jvm/main"),
                File("empty/build/classes/java/jvmMain"),
                File("empty/build/processedResources/jvm/main"),
                File("transformed/uklib_jar_fragment.jar"),
                File("kotlin-stdlib/${defaultBuildOptions.kotlinVersion}/kotlin-stdlib-${defaultBuildOptions.kotlinVersion}.jar"),
                File("annotations-13.0.jar"),
            )
            assertEquals(
                runJvmClasspath.size,
                matchers.size,
                message = runJvmClasspath.toString()
            )
            val unmatchedClasspath = matchers.zip(runJvmClasspath) { matcher, classpathElement ->
                runCatching {
                    assertTrue(
                        classpathElement.endsWith(matcher),
                        message = "${classpathElement} endsWith ${matcher}"
                    )
                }
            }.mapNotNull {
                it.exceptionOrNull()
            }
            if (!unmatchedClasspath.isEmpty()) {
                val exception = AssertionError("Unmatched classpath")
                unmatchedClasspath.forEach(exception::addSuppressed)
                throw exception
            }
        }
    }

    @GradleTest
    fun `uklib consumption - resolve uklib dependency for IDE`(
        version: GradleVersion,
    ) {
        val producerGroup = "producer"
        val targetSetup: KotlinMultiplatformExtension.() -> Unit = {
            linuxArm64()
            linuxX64()
            jvm()
        }
        val direct = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    targetSetup()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = producerGroup))

        val consumer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(direct)
            buildScriptInjection {
                project.computeTransformedLibraryChecksum()
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    targetSetup()
                    sourceSets.commonMain.get().dependencies { implementation(direct.rootCoordinate) }
                }
            }
        }
        consumer.resolveIdeDependencies { ideDependencies ->
            data class RelativePath(
                val components: List<String>
            )
            data class Coordinate(
                val group: String?,
                val artifact: String?,
                val version: String?,
                val fragmentName: String?
            )

            fun normalizedProducerDependency(sourceSetName: String) = ideDependencies[sourceSetName]
                .filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
                .filter { it.coordinates?.group == producerGroup }
                .map { dependency ->
                    val coordinates = dependency.coordinates ?: error("Missing coordinate")
                    Coordinate(
                        group = coordinates.group,
                        artifact = coordinates.module,
                        version = coordinates.version,
                        fragmentName = coordinates.sourceSetName,
                    ) to dependency.classpath.map {
                        RelativePath(
                            it.relativeTo(consumer.projectPath.toFile().canonicalFile)
                                .toPath().toList().takeLast(2).map { it.pathString }
                        )
                    }
                }
                .single()

            val expected = mapOf<String, Pair<Coordinate, List<RelativePath>>>(
                "commonMain" to Pair(
                    first = Coordinate("producer", "empty", "1.0", "commonMain"),
                    second = mutableListOf(
                        RelativePath(
                            mutableListOf("kotlinTransformedMetadataLibraries", "uklib-producer-empty-1.0-commonMain-",),
                        ),
                    ),
                ),
                "jvmMain" to Pair(
                    first = Coordinate("producer", "empty", "1.0", "jvm"),
                    second = mutableListOf(
                        RelativePath(
                            mutableListOf("transformed", "uklib_jar_fragment.jar"),
                        ),
                    ),
                ),
                "linuxArm64Main" to Pair(
                    first = Coordinate("producer", "empty", "1.0", "linux_arm64"),
                    second = mutableListOf(
                        RelativePath(
                            mutableListOf(
                                "unzipped_uklib_empty.uklib", "linuxArm64Main",
                            ),
                        ),
                    ),
                ),
            )

            assertEquals(
                expected.prettyPrinted,
                expected.mapValues {
                    normalizedProducerDependency(it.key)
                }.prettyPrinted
            )
        }
    }

    @GradleTest
    fun `uklib consumption - jvm resolution through non-jvm uklib producer`(version: GradleVersion) {
        val transitiveJvmProducer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.plugins.apply("java-library")
            }
        }.publishJava(PublisherConfiguration(group = "producer"))

        val intermediateUklibProducer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(transitiveJvmProducer)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileSource("class Common")
                    sourceSets.commonMain.get().dependencies { api(transitiveJvmProducer.rootCoordinate) }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "intermediate"))

        val kmpJvmConsumer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(transitiveJvmProducer)
            addPublishedProjectToRepositories(intermediateUklibProducer)
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    sourceSets.commonMain.get().dependencies { implementation(intermediateUklibProducer.rootCoordinate) }
                }
            }
        }
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "intermediate:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.gradle.jvm.environment" to "standard-jvm",
                        ),
                    ),
                    configuration = "javaApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmApiElements",
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "compile",
                ),
                "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.dependency.bundling" to "external",
                            "org.gradle.jvm.version" to "17",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "apiElements",
                ),
            ).prettyPrinted,
            kmpJvmConsumer.buildScriptReturn {
                project.ignoreAccessViolations {
                    project.configurations.getByName(
                        project.multiplatformExtension.jvm().compilations.getByName("main").compileDependencyConfigurationName
                    ).resolveProjectDependencyComponentsWithArtifacts()
                }
            }.buildAndReturn("assemble").prettyPrinted
        )
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "intermediate:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                            "org.gradle.jvm.environment" to "standard-jvm",
                        ),
                    ),
                    configuration = "javaRuntimeElements",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmRuntimeElements",
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                        ),
                    ),
                    configuration = "runtime",
                ),
                "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.dependency.bundling" to "external",
                            "org.gradle.jvm.version" to "17",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                        ),
                    ),
                    configuration = "runtimeElements",
                ),
            ).prettyPrinted,
            kmpJvmConsumer.buildScriptReturn {
                project.ignoreAccessViolations {
                    project.configurations.getByName(
                        project.multiplatformExtension.jvm().compilations.getByName("main").runtimeDependencyConfigurationName
                    ).resolveProjectDependencyComponentsWithArtifacts()
                }
            }.buildAndReturn("assemble").prettyPrinted
        )

        val javaConsumer = project("empty", version) {
            addPublishedProjectToRepositories(transitiveJvmProducer)
            addPublishedProjectToRepositories(intermediateUklibProducer)
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.plugins.apply("java")
                dependencies.add("implementation", intermediateUklibProducer.rootCoordinate)
            }
        }
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "intermediate:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.gradle.jvm.environment" to "standard-jvm",
                        ),
                    ),
                    configuration = "javaApiElements",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmApiElements",
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "compile",
                ),
                "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.dependency.bundling" to "external",
                            "org.gradle.jvm.version" to "17",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "apiElements",
                ),
            ).prettyPrinted,
            javaConsumer.buildScriptReturn {
                project.ignoreAccessViolations {
                    project.configurations.getByName("compileClasspath")
                        .resolveProjectDependencyComponentsWithArtifacts()
                }
            }.buildAndReturn("assemble").prettyPrinted
        )
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "intermediate:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                            "org.gradle.jvm.environment" to "standard-jvm",
                        ),
                    ),
                    configuration = "javaRuntimeElements",
                ),
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmRuntimeElements",
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                        ),
                    ),
                    configuration = "runtime",
                ),
                "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.dependency.bundling" to "external",
                            "org.gradle.jvm.version" to "17",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                        ),
                    ),
                    configuration = "runtimeElements",
                ),
            ).prettyPrinted,
            javaConsumer.buildScriptReturn {
                project.ignoreAccessViolations {
                    project.configurations.getByName("runtimeClasspath")
                        .resolveProjectDependencyComponentsWithArtifacts()
                }
            }.buildAndReturn("assemble").prettyPrinted
        )
    }

    @GradleTest
    fun `uklib consumption - MR jar`(version: GradleVersion) {
        val producer = project(
            "empty",
            version,
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                // Commonizer is not supported yet which will be captured by this test
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    jvm {
                        project.tasks.named(artifactsTaskName, Jar::class.java) {
                            it.manifest {
                                it.attributes(mapOf("Multi-Release" to true))
                            }
                            it.rename(".*module-info.class.*", "META-INF/versions/9/module-info.class")
                        }
                        compilations.getByName("main").compileJavaTaskProvider?.configure {
                            it.sourceCompatibility = "9"
                            it.targetCompatibility = "9"
                        }
                        compilerOptions {
                            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_9)
                        }
                    }
                    val jvmSources = project.layout.projectDirectory.file("src/jvmMain/java").asFile
                    jvmSources.mkdirs()
                    val producer = jvmSources.resolve("producer/Producer.java")
                    producer.parentFile.mkdirs()
                    producer.writeText("""
                        package producer;
                        
                        public class Producer { }
                    """.trimIndent())
                    val moduleInfo = jvmSources.resolve("module-info.java")
                    moduleInfo.parentFile.mkdirs()
                    moduleInfo.writeText(
                        """
                            module producer {
                                requires transitive kotlin.stdlib;
                                
                                exports producer;
                            }
                        """.trimIndent()
                    )
                }
            }
        }.publish()

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(producer)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    jvm {
                        compilations.getByName("main").compileJavaTaskProvider?.configure {
                            it.sourceCompatibility = "9"
                            it.targetCompatibility = "9"
                        }
                        compilerOptions {
                            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_9)
                        }
                    }
                    val jvmSources = project.layout.projectDirectory.file("src/jvmMain/kotlin").asFile
                    jvmSources.mkdirs()

                    val moduleInfo = jvmSources.resolve("module-info.java")
                    moduleInfo.parentFile.mkdirs()
                    moduleInfo.writeText(
                        """
                            module consumer {
                                requires transitive kotlin.stdlib;
                                requires producer;
                            }
                        """.trimIndent()
                    )

                    sourceSets.jvmMain.get().compileSource("""
                        fun consume() {
                            producer.Producer()
                        }
                    """.trimIndent())

                    sourceSets.commonMain.get().dependencies {
                        implementation(producer.rootCoordinate)
                    }
                }
            }

            build("assemble")
        }
    }

    @GradleTest
    fun `uklib consumption - linkage with cinterops`(version: GradleVersion) {
        val producer = project(
            "empty",
            version,
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                // Commonizer is not supported yet which will be captured by this test
                project.extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION, true)
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    listOf(
                        linuxArm64(),
                        linuxX64(),
                    ).forEach {
                        val foo = project.layout.projectDirectory.file("foo.def")
                        val bar = project.layout.projectDirectory.file("bar.def")

                        foo.asFile.writeText(
                            """
                                language = C
                                ---
                                void foo(void);
                            """.trimIndent()
                        )
                        bar.asFile.writeText(
                            """
                                language = C
                                ---
                                void bar(void);
                            """.trimIndent()
                        )

                        it.compilations.getByName("main").cinterops.create("foo") {
                            it.definitionFile.set(foo)
                        }
                        it.compilations.getByName("main").cinterops.create("bar") {
                            it.definitionFile.set(bar)
                        }
                    }

                    sourceSets.commonMain.get().compileSource("class Common")
                }
            }
        }.publish()

        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(producer)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64 {
                        binaries.staticLib {  }
                    }
                    sourceSets.commonMain.get().compileSource(
                        """
                        @file:OptIn(ExperimentalForeignApi::class)

                        import kotlinx.cinterop.ExperimentalForeignApi

                        fun consumeCinterops() { 
                            bar.bar()
                            foo.foo()
                        }
                        """.trimIndent()
                    )
                    sourceSets.commonMain.get().dependencies {
                        api(producer.rootCoordinate)
                    }
                }
            }

            build("linkDebugStaticLinuxArm64")
        }
    }

    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88)
    fun `uklib consumption - KMP androidLibrary resolves to fallback variant with pre-UKlib dependencies`(
        version: GradleVersion,
        androidVersion: String,
    ) {
        val configureAndroidLibrary: KotlinMultiplatformExtension.() -> Unit = {
            val target = targets.getByName("android")
            val klass = target::class.java.classLoader.loadClass("com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension")
            val compileSdk = klass.getMethod("setCompileSdk", Int::class.javaObjectType)
            compileSdk.invoke(target, 31)
            val namespace = klass.getMethod("setNamespace", String::class.java)
            namespace.invoke(target, "foo")
        }
        val producer = project(
            "empty",
            version,
        ) {
            addKgpToBuildScriptCompilationClasspath()
            addAgpToBuildScriptCompilationClasspath(androidVersion)
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()
                    js()
                    sourceSets.commonMain.get().compileSource("class Common")
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer"))

        val consumer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addAgpToBuildScriptCompilationClasspath(androidVersion)
            addPublishedProjectToRepositories(producer)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.setUklibPublicationStrategy()
                project.plugins.apply("com.android.kotlin.multiplatform.library")
                project.applyMultiplatform {
                    configureAndroidLibrary()
                    sourceSets.commonMain.dependencies {
                        implementation(producer.rootCoordinate)
                    }
                }
            }
        }

        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmApiElements",
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "compile",
                ),
                "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "fallbackVariant_KT-81412",
                ),
            ).prettyPrinted,
            consumer.buildScriptReturn {
                project.ignoreAccessViolations {
                    project.configurations.getByName("androidCompileClasspath").resolveProjectDependencyComponentsWithArtifacts()
                }
            }.buildAndReturn("assemble").prettyPrinted
        )
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmRuntimeElements",
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                        ),
                    ),
                    configuration = "runtime",
                ),
                "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "fallbackVariant_KT-81412",
                ),
            ).prettyPrinted,
            consumer.buildScriptReturn {
                project.ignoreAccessViolations {
                    project.configurations.getByName("androidRuntimeClasspath").resolveProjectDependencyComponentsWithArtifacts()
                }
            }.buildAndReturn("assemble").prettyPrinted
        )
    }

    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_88)
    fun `uklib consumption - KMP androidLibrary with stub JVM variant - KT-81434`(
        version: GradleVersion,
        androidVersion: String,
    ) {
        val configureAndroidLibrary: KotlinMultiplatformExtension.() -> Unit = {
            val target = targets.getByName("android")
            val klass = target::class.java.classLoader.loadClass("com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension")
            val compileSdk = klass.getMethod("setCompileSdk", Int::class.javaObjectType)
            compileSdk.invoke(target, 31)
            val namespace = klass.getMethod("setNamespace", String::class.java)
            namespace.invoke(target, "foo")
        }
        val producer = project(
            "empty",
            version,
        ) {
            addKgpToBuildScriptCompilationClasspath()
            addAgpToBuildScriptCompilationClasspath(androidVersion)
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.setUklibResolutionStrategy()
                project.plugins.apply("com.android.kotlin.multiplatform.library")
                project.applyMultiplatform {
                    configureAndroidLibrary()
                    linuxArm64()
                    sourceSets.commonMain.get().compileSource("class Common")
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer"))

        val consumer = project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            addAgpToBuildScriptCompilationClasspath(androidVersion)
            addPublishedProjectToRepositories(producer)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.setUklibPublicationStrategy()
                project.plugins.apply("com.android.kotlin.multiplatform.library")
                project.applyMultiplatform {
                    configureAndroidLibrary()
                    sourceSets.commonMain.dependencies {
                        (implementation(producer.rootCoordinate) as ModuleDependency).isTransitive = false
                    }
                }
            }
        }

        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmApiElements",
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-api",
                        ),
                    ),
                    configuration = "compile",
                ),
                "producer:empty-android:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "aar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "android",
                            "org.gradle.libraryelements" to "aar",
                            "org.gradle.usage" to "java-api",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "androidApiElements-published",
                ),
                "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "androidApiElements-published",
                ),
            ).prettyPrinted,
            consumer.buildScriptReturn {
                project.ignoreAccessViolations {
                    project.configurations.getByName("androidCompileClasspath").resolveProjectDependencyComponentsWithArtifacts()
                }
            }.buildAndReturn("assemble").prettyPrinted
        )
        assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
            mutableMapOf<String, ResolvedComponentWithArtifacts>(
                "org.jetbrains.kotlin:kotlin-stdlib:${defaultBuildOptions.kotlinVersion}" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "standard-jvm",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "jvmRuntimeElements",
                ),
                "org.jetbrains:annotations:13.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "jar",
                            "org.gradle.category" to "library",
                            "org.gradle.libraryelements" to "jar",
                            "org.gradle.usage" to "java-runtime",
                        ),
                    ),
                    configuration = "runtime",
                ),
                "producer:empty-android:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                        mutableMapOf(
                            "artifactType" to "aar",
                            "org.gradle.category" to "library",
                            "org.gradle.jvm.environment" to "android",
                            "org.gradle.libraryelements" to "aar",
                            "org.gradle.usage" to "java-runtime",
                            "org.jetbrains.kotlin.platform.type" to "jvm",
                        ),
                    ),
                    configuration = "androidRuntimeElements-published",
                ),
                "producer:empty:1.0" to ResolvedComponentWithArtifacts(
                    artifacts = mutableListOf(
                    ),
                    configuration = "androidRuntimeElements-published",
                ),
            ).prettyPrinted,
            consumer.buildScriptReturn {
                project.ignoreAccessViolations {
                    project.configurations.getByName("androidRuntimeClasspath").resolveProjectDependencyComponentsWithArtifacts()
                }
            }.buildAndReturn("assemble").prettyPrinted
        )
    }

}
