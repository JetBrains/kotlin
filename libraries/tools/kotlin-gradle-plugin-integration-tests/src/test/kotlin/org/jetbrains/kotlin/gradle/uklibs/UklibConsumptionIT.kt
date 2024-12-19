/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.GradleInternal
import org.gradle.api.internal.project.ProjectStateRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation
import org.jetbrains.kotlin.gradle.plugin.mpp.locateOrRegisterMetadataDependencyTransformationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.UnzippedUklibToPlatformCompilationTransform
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.io.Serializable
import kotlin.io.path.pathString
import kotlin.test.assertEquals

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
            androidTarget().publishAllLibraryVariants()
            linuxArm64()
            iosArm64()
            iosX64()
            macosArm64()
            jvm()
            js()
            wasmJs()
            wasmWasi()
        }
        val publisher = publishUklib(gradleVersion, androidVersion) {
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
            "buildScriptInjectionGroovyWithAGP",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = androidVersion,
            ),
        ) {
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(publisher)
            buildScriptInjection {
                project.enableCrossCompilation()
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
                        val arguments = producerTypes.map { "${it}: ${it}" }.joinToString(", ")
                        it.compileSource(
                            """
                                fun consumeIn_${it.name}(${arguments}) {}
                            """.trimIndent()
                        )
                    }

                    sourceSets.commonMain.dependencies {
                        implementation(publisher.coordinate)
                    }
                    sourceSets.androidMain.dependencies {
                        // FIXME: Why is AGP publication not renamed?
                        implementation("${publisher.group}:buildScriptInjectionGroovyWithAGP-android:${publisher.version}")
                    }
                }
            }

            // FIXME: Maybe just nuke Gradle metadata?
            val resolvedProducerIosArm64Variant = buildScriptReturn {
                (project.gradle as GradleInternal).services.get(ProjectStateRegistry::class.java).allowUncontrolledAccessToAnyProject {
                    val selectedVariant = kotlinMultiplatform.iosArm64().compilations.getByName("main")
                        .configurations.compileDependencyConfiguration
                        .incoming.artifacts.artifacts.single {
                            val identifier = (it.id.componentIdentifier as? ModuleComponentIdentifier) ?: return@single false
                            identifier.group == publisher.group
                                    && identifier.module == publisher.name
                                    && identifier.version == publisher.version
                        }.variant
                    selectedVariant.attributes.keySet().map {
                        it.toString() to selectedVariant.attributes.getAttribute(it).toString()
                    }.toMap()
                }
            }.buildAndReturn()

            // Make sure that we are actually resolving uklib from metadataSources configuration
            assertEquals(
                resolvedProducerIosArm64Variant["org.jetbrains.kotlin.uklibView"],
                "ios_arm64",
            )

            // FIXME: Run test compilations
            build("assemble")

            data class KlibsToCheck(
                val iosArm64Klib: File,
                val commonMainKlib: File,
            ) : Serializable

            val klibs = buildScriptReturn {
                KlibsToCheck(
                    kotlinMultiplatform.iosArm64().compilations.getByName("main").compileTaskProvider.flatMap { it.outputFile }.get(),
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
            if (androidVersion != null) "buildScriptInjectionGroovyWithAGP" else "buildScriptInjectionGroovy",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
        ) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    publisherConfiguration()
                }
            }
        }.publish(PublisherConfiguration())
    }

    @GradleTest
    fun `uklib consumption - with a subset of targets - in a source set with a superset of targets`(
        version: GradleVersion,
    ) {
        val publisher = project("buildScriptInjectionGroovy", version) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    sourceSets.commonMain.get().addIdentifierClass()
                }
            }
        }.publish(PublisherConfiguration(name = "dependency"))

        project("buildScriptInjectionGroovy", version) {
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(publisher)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.all { it.addIdentifierClass() }
                    sourceSets.commonMain.dependencies {
                        implementation(publisher.coordinate)
                    }
                }
            }

            val jvmMainCompilationTask = buildScriptReturn {
                kotlinMultiplatform.jvm().compilations.getByName("main").compileTaskProvider.name
            }.buildAndReturn()
            val jvmTransformationException = catchBuildFailures<UnzippedUklibToPlatformCompilationTransform.PlatformCompilationTransformException>().buildAndReturn(
                evaluationTask = jvmMainCompilationTask,
            ).unwrap().single()
            assertEquals(
                "jvm",
                jvmTransformationException.targetFragmentAttribute,
            )
            assertEquals(
                listOf("commonMain", "linuxArm64Main", "linuxX64Main"),
                jvmTransformationException.availablePlatformFragments,
            )

            val commonMainTransformationTask = buildScriptReturn {
                project.locateOrRegisterMetadataDependencyTransformationTask(
                    kotlinMultiplatform.sourceSets.commonMain.get()
                ).get().name
            }.buildAndReturn()
            val commonMainTransformationException = catchBuildFailures<GranularMetadataTransformation.UklibIsMissingRequiredAttributesException>().buildAndReturn(
                evaluationTask = commonMainTransformationTask,
            ).unwrap().single()
            assertEquals(
                listOf("jvm", "linux_arm64", "linux_x64"),
                commonMainTransformationException.targetFragmentAttribute,
            )
            assertEquals(
                listOf("commonMain", "linuxArm64Main", "linuxX64Main"),
                commonMainTransformationException.availablePlatformFragments,
            )
        }
    }

    @GradleTest
    fun `uklib consumption - with a subset of targets`(
        version: GradleVersion,
    ) {
        val publisher = project("buildScriptInjectionGroovy", version) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    sourceSets.commonMain.get().addIdentifierClass()
                }
            }
        }.publish(PublisherConfiguration(name = "dependency"))

        project("buildScriptInjectionGroovy", version) {
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(publisher)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.all { it.addIdentifierClass() }
                    sourceSets.iosMain.dependencies {
                        implementation(publisher.coordinate)
                    }
                }
            }

            build("assemble")
        }
    }

    @GradleTest
    fun `uklib consumption - transitive uklib is consumed through a jvm dependency`(
        version: GradleVersion,
    ) {
        val transitive = project("buildScriptInjectionGroovy", version) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.linuxMain.get().addIdentifierClass()
                    sourceSets.commonMain.get().addIdentifierClass()
                }
            }
        }.publish(PublisherConfiguration(name = "transitive"))

        val direct = project("buildScriptInjectionGroovy", version) {
            addPublishedProjectToRepositories(transitive)
            buildScriptInjection {
                project.plugins.apply("java-library")
                java.sourceSets.all {
                    it.compileJavaSource(
                        project,
                        """
                            public class Direct_${it.name} { }
                        """.trimIndent()
                    )
                }
                project.configurations.getByName("api").dependencies.add(
                    project.dependencies.create(transitive.coordinate)
                )
            }
        }.publishJava(PublisherConfiguration(name = "direct"))

        project("buildScriptInjectionGroovy", version) {
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(direct)
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(transitive)
            buildScriptInjection {
                project.computeUklibChecksum(false)
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.all { it.addIdentifierClass() }
                    sourceSets.commonMain.dependencies {
                        implementation(direct.coordinate)
                    }
                }
            }

            build("assemble")

            val classpath = buildScriptReturn {
                val transformationTask = project.locateOrRegisterMetadataDependencyTransformationTask(
                    kotlinMultiplatform.sourceSets.getByName("commonMain")
                ).get()
                transformationTask.allTransformedLibraries().get()
            }.buildAndReturn(
                buildScriptReturn {
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        kotlinMultiplatform.sourceSets.getByName("commonMain")
                    ).get().name
                }.buildAndReturn()
            )

            assertEquals(
                listOf(
                    listOf("foo", "direct", "1.0", "direct-1.0.jar"),
                    listOf("build", "kotlinTransformedMetadataLibraries", "commonMain", "uklib-foo-transitive-1.0-commonMain-"),
                ),
                classpath.filterNot {
                    "kotlin-stdlib" in it.name
                }.map {
                    it.toPath().toList().takeLast(4).map { it.pathString }
                }
            )
        }
    }

    /**
     *  Should this even be allowed to publish or should it lead to resolution error?
     *  Actually maybe also try consuming outside common?
     *
     */
    @GradleTest
    fun `uklib consumption - publish only jvm and linuxMain and consume in common`(
        version: GradleVersion,
    ) {
        val publisher = project("buildScriptInjectionGroovy", version) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.linuxMain.get().addIdentifierClass()
                    sourceSets.jvmMain.get().addIdentifierClass()
                }
            }
        }.publish(PublisherConfiguration())

        project("buildScriptInjectionGroovy", version) {
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(publisher)
            buildScriptInjection {
                project.setUklibResolutionStrategy()
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    jvm()
                    sourceSets.all { it.addIdentifierClass() }
                    sourceSets.commonMain.dependencies {
                        implementation(publisher.coordinate)
                    }
                }
            }

            val commonMainTransformationException = catchBuildFailures<GranularMetadataTransformation.UklibIsMissingRequiredAttributesException>().buildAndReturn(
                evaluationTask = "assemble",
            ).unwrap().single()
            assertEquals(
                setOf("jvm", "linux_arm64", "linux_x64"),
                commonMainTransformationException.targetFragmentAttribute.toSet(),
            )
            assertEquals(
                setOf("linuxArm64Main", "linuxMain", "linuxX64Main", "jvmMain"),
                commonMainTransformationException.availablePlatformFragments.toSet(),
            )
        }
    }

    @GradleAndroidTest
    fun `uklib consumption - androidTarget consumes jvm only uklib`(
        version: GradleVersion,
        agpVersion: String,
    ) {
        val direct = project("buildScriptInjectionGroovy", version) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    jvm()
                    sourceSets.commonMain.get().compileSource("class Jvm")
                }
            }
        }.publish(PublisherConfiguration(name = "transitive"))

        project("buildScriptInjectionGroovyWithAGP", version) {
            addPublishedProjectToRepositoriesAndIgnoreGradleMetadata(direct)
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
                        implementation(direct.coordinate)
                    }
                }
            }

            // FIXME: Is this actually supposed to pass
            // FIXME: Catch specific compilation failure or reproduce the resolution error
            buildAndFail("assemble", buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion))
        }
    }
}