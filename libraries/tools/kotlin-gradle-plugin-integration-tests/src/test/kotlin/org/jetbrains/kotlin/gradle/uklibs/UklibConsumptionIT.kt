/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.internal.GradleInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.internal.project.*
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.artifacts.UklibResolutionStrategy
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.internal.compilerRunner.native.nativeCompilerClasspath
import org.jetbrains.kotlin.library.impl.buffer
import org.junit.jupiter.api.DisplayName
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.net.URLClassLoader
import kotlin.test.assertEquals
import java.io.PrintStream
import java.nio.channels.Pipe
import kotlin.concurrent.thread

@MppGradlePluginTests
@DisplayName("Smoke test uklib consumption")
class UklibConsumptionIT : KGPBaseTest() {

    @GradleTest
    fun `uklib consumption smoke - in kotlin compilations of a symmetric consumer and producer projects - with all metadata compilations`(
        version: GradleVersion
    ) {
        val symmetricTargets: KotlinMultiplatformExtension.() -> Unit = @JvmSerializableLambda {
            linuxArm64()
            iosArm64()
            iosX64()
            jvm()
            js()
            wasmJs()
            wasmWasi()
        }
        val publisher = publishUklib(version) @JvmSerializableLambda {
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
        ).flatMap {
            listOf(
                it.key + "Main" to it.value,
                it.key + "Test" to it.value,
            )
        }.toMap()

        project(
            "buildScriptInjectionGroovy",
            version,
            dependencyManagement = DependencyManagement.DefaultDependencyManagement(
                gradleRepositoriesMode = RepositoriesMode.PREFER_PROJECT,
            )
        ) {
            addPublishedProjectToRepositories(publisher) @JvmSerializableLambda {
                metadataSources {
                    it.mavenPom()
                    it.ignoreGradleMetadataRedirection()
                }
            }
            buildScriptInjection {
                project.propertiesExtension.set(
                    PropertiesProvider.PropertyNames.KOTLIN_MPP_UKLIB_RESOLUTION_STRATEGY,
                    UklibResolutionStrategy.AllowResolvingUklibs.propertyName
                )
                project.applyMultiplatform {
                    symmetricTargets()

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
                        implementation("${publisher.group}:${publisher.name}:${publisher.version}")
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
                resolvedProducerIosArm64Variant["uklibDestination"],
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

    private fun TestProject.dumpKlibMetadataSignatures(klib: File): String {
        val dumpName = "dump_${klib.name}"
        // FIXME: This path is not actually unique
        val outputFile = projectPath.resolve(dumpName).toFile()
        outputFile.createNewFile()

        buildScriptInjection {
            project.tasks.register(dumpName) {
                val inputs = project.objects.nativeCompilerClasspath(
                    project.nativeProperties.actualNativeHomeDirectory,
                    project.nativeProperties.shouldUseEmbeddableCompilerJar,
                )
                it.inputs.files(inputs)
                val nativePath = project.nativeProperties.actualNativeHomeDirectory
                it.doLast {
                    // FIXME: Why does App class loader loads compiler classes and why is this crutch needed ???
                    System.setProperty("kotlin.native.home", nativePath.get().path)
                    URLClassLoader(inputs.map { it.toURI().toURL() }.toTypedArray()).use { classLoader ->
                        val entryPoint = Class.forName("org.jetbrains.kotlin.cli.klib.Main", true, classLoader)
                            .declaredMethods
                            .single { it.name == "exec" }

                        val stdout = PrintStream(FileOutputStream(outputFile))
                        val stderr = System.err

                        val args: Array<String> = arrayOf(
                            "dump-metadata-signatures", klib.path,
                            "-test-mode", "true",
                        )

                        assert(entryPoint.invoke(null, stdout, stderr, args) as Int == 0)
                    }
                }
            }
        }

        build(dumpName)

        return outputFile.readText()
    }

    private fun publishUklib(
        gradleVersion: GradleVersion,
        publisherConfiguration: KotlinMultiplatformExtension.() -> Unit,
    ): PublishedProject {
        return project<PublishedProject>(
            "buildScriptInjectionGroovy",
            gradleVersion,
        ) {
            buildScriptInjection {
                project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_PUBLISH_UKLIB, true.toString())
                project.applyMultiplatform {
                    publisherConfiguration()
                }
            }
            publish(PublisherConfiguration())
        }.result
    }
}

val Project.propertiesExtension: ExtraPropertiesExtension
    get() = extensions.getByType(ExtraPropertiesExtension::class.java)