/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.uklibs.*
import kotlin.test.assertEquals

@MppGradlePluginTests
class BuildScriptInjectionIT : KGPBaseTest() {

    @GradleTestVersions
    @GradleTest
    fun publishAndConsumeKtsTemplate(version: GradleVersion) {
        publishAndConsumeProject(
            "buildScriptInjection",
            version,
        )
    }

    @GradleTestVersions
    @GradleTest
    fun publishAndConsumeGroovyTemplate(version: GradleVersion) {
        publishAndConsumeProject(
            "buildScriptInjectionGroovy",
            version,
        )
    }

    @GradleTestVersions
    @GradleTest
    fun consumeProjectDependencyViaSettingsInjection(version: GradleVersion) {
        // Use Groovy because it loads faster
        project("buildScriptInjectionGroovy", version) {
            val producer = project("buildScriptInjectionGroovy", version) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        linuxArm64()
                        linuxX64()
                        sourceSets.commonMain.get().compileSource("class Common")
                        sourceSets.linuxArm64Main.get().compileSource("class LinuxArm64")
                    }
                }
            }
            val consumer = project("buildScriptInjectionGroovy", version) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        linuxArm64()
                        linuxX64()
                        sourceSets.commonMain {
                            compileSource(
                                """
                                    fun consumeInCommon(common: Common) {}
                                """.trimIndent()
                            )
                            dependencies {
                                implementation(project(":producer"))
                            }
                        }
                        sourceSets.linuxArm64Main.get().compileSource(
                            """
                                fun consumeInLinuxArm64Main(common: Common, linuxArm64: LinuxArm64) {}
                            """.trimIndent()
                        )
                    }
                }
            }

            include(consumer, "consumer")
            include(producer, "producer")

            build(":consumer:assemble")

            assertEquals(
                """
                /consumeInCommon|consumeInCommon(Common){}[0]
                
                """.trimIndent(),
                dumpKlibMetadataSignatures(
                    consumer.buildScriptReturn {
                        kotlinMultiplatform.metadata().compilations.getByName("commonMain").output.classesDirs.singleFile
                    }.buildAndReturn(executingProject = this)
                ),
            )

            assertEquals(
                """
                /consumeInCommon|consumeInCommon(Common){}[0]
                /consumeInLinuxArm64Main|consumeInLinuxArm64Main(Common;LinuxArm64){}[0]
                
                """.trimIndent(),
                dumpKlibMetadataSignatures(
                    consumer.buildScriptReturn {
                        kotlinMultiplatform.linuxArm64().compilations.getByName("main").output.classesDirs.singleFile
                    }.buildAndReturn(executingProject = this)
                ),
            )
        }
    }

    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_8_1,
    )
    @GradleTest
    fun buildScriptReturnIsCCFriendly(version: GradleVersion) {
        project("buildScriptInjectionGroovy", version) {
            buildScriptReturn {
                project.layout.projectDirectory.file("foo").asFile
            }.buildAndReturn(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
            )
        }
    }

    private fun publishAndConsumeProject(
        targetProject: String,
        version: GradleVersion,
    ) {
        val publishedProject = runTestProject(
            targetProject,
            version,
        ) {
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    sourceSets.all {
                        it.compileSource("class ${it.name}")
                    }
                }
            }
            publish(PublisherConfiguration())
        }.result

        project(
            targetProject,
            version,
        ) {
            addPublishedProjectToRepositories(publishedProject)
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()

                    sourceSets.all {
                        // Test compilations will fail
                        it.compileSource("fun consumeIn_${it.name}() { ${it.name}() }")
                    }

                    sourceSets.commonMain.dependencies {
                        implementation(publishedProject.coordinate)
                    }
                }
            }

            val metadataTransformationTaskName = buildScriptReturn {
                with(kotlinMultiplatform) {
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        sourceSets.commonMain.get()
                    ).get().name
                }
            }.buildAndReturn()

            val transformedFiles = buildScriptReturn {
                with(kotlinMultiplatform) {
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        sourceSets.commonMain.get()
                    ).get().allTransformedLibraries().get()
                }
            }.buildAndReturn(metadataTransformationTaskName)

            assertEquals(
                listOf(
                    listOf("foo", "producer", "1.0", "linuxMain"),
                    listOf("foo", "producer", "1.0", "nativeMain"),
                    listOf("foo", "producer", "1.0", "commonMain"),
                ),
                transformedFiles.map { it.nameWithoutExtension.split("-").take(4) },
            )
        }
    }
}