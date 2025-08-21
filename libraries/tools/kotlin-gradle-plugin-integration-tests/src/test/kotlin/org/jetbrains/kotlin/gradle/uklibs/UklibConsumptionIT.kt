/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import com.android.build.api.dsl.LibraryExtension
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.io.Serializable
import kotlin.io.path.pathString
import kotlin.test.assertEquals

@OptIn(ExperimentalWasmDsl::class)
@MppGradlePluginTests
@DisplayName("Smoke test uklib consumption")
class UklibConsumptionIT : KGPBaseTest() {

    // FIXME: This test should run with Android, but due to KT-76265 it currently OOMs
    // FIXME: As a temporary workaround add a test featuring only Android + metadata compilation to check that Android in a metadata
    // compilation is correctly consumed
    // @GradleAndroidTest
    @GradleTest
    fun `uklib consumption smoke - in kotlin compilations of a symmetric consumer and producer projects - with all metadata compilations`(
        gradleVersion: GradleVersion,
        // androidVersion: String,
    ) {
        val symmetricTargets: KotlinMultiplatformExtension.() -> Unit = {
            // androidTarget().publishLibraryVariants("debug", "release")
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
            // androidVersion = androidVersion
        ) {
            // project.plugins.apply("com.android.library")
            // with(project.extensions.getByType(LibraryExtension::class.java)) {
            //    compileSdk = 23
            //    namespace = "kotlin.producer"
            // }

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
            // Due to KT-76265, on 1GB heap this test sometimes OOMs on CI even without androidTarget
            enableGradleDaemonMemoryLimitInMb = 1024 * 2,
            buildOptions = defaultBuildOptions.copy(
                // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
                isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
                // androidVersion = androidVersion,
            ),
        ) {
            // addAgpToBuildScriptCompilationClasspath(androidVersion)
            addKgpToBuildScriptCompilationClasspath()
            addPublishedProjectToRepositories(publisher)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                // project.plugins.apply("com.android.library")
                // with(project.extensions.getByType(LibraryExtension::class.java)) {
                //    compileSdk = 23
                //    namespace = "kotlin.consumer"
                // }

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
                                "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
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
                                "org.jetbrains.kotlin.isMetadataJar" to "not-a-metadata-jar",
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
}
