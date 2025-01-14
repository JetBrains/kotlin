/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.OutputFile
import org.gradle.api.plugins.UnknownPluginException
import org.gradle.testkit.runner.UnexpectedBuildSuccess
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.uklibs.*
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.fail

@MppGradlePluginTests
class BuildScriptInjectionIT : KGPBaseTest() {

    @GradleTest
    fun publishAndConsumeKtsTemplate(version: GradleVersion) {
        publishAndConsumeProject(
            "emptyKts",
            version,
        )
    }

    @GradleTest
    fun publishAndConsumeGroovyTemplate(version: GradleVersion) {
        publishAndConsumeProject(
            "empty",
            version,
        )
    }

    @GradleTest
    fun consumeProjectDependencyViaSettingsInjection(version: GradleVersion) {
        // Use Groovy because it loads faster
        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            val producer = project("empty", version) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        linuxArm64()
                        linuxX64()
                        sourceSets.commonMain.get().compileSource("class Common")
                        sourceSets.linuxArm64Main.get().compileSource("class LinuxArm64")
                    }
                }
            }
            val consumer = project("empty", version) {
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
        // Sanity check that enabling CC produces CC serialization errors with inappropriately constructed providers in providerBuildScriptReturn
        project("empty", version) {
            val returnValue = providerBuildScriptReturn {
                project.provider { project }
            }
            buildAndFail(
                "tasks", "-P${returnValue.injectionLoadProperty}",
                buildOptions = defaultBuildOptions.withConfigurationCache,
            ) {
                assertOutputContains("cannot serialize object of type")
            }
        }

        // Check that in the simple case we don't fail to return value with CC
        project("empty", version) {
            buildScriptReturn {
                project.layout.projectDirectory.file("foo").asFile
            }.buildAndReturn(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
            )
        }

        // Make sure delayed task providers (e.g. mapped from a task) get queried at the correct time even in runs with CC
        abstract class MappedTaskOutput : DefaultTask() {
            @get:OutputFile
            abstract val out: RegularFileProperty
        }
        project("empty", version) {
            val taskName = "foo"
            val produceCCSerializationError = "produceCCSerializationError"
            val mappedTaskOutputProvider: GradleProjectBuildScriptInjectionContext.() -> Provider<File> = {
                project.tasks.named(taskName, MappedTaskOutput::class.java).flatMap {
                    it.out
                }.map {
                    it.asFile
                }
            }

            buildScriptInjection {
                project.tasks.register(taskName, MappedTaskOutput::class.java) {
                    it.out.set(project.layout.buildDirectory.file("foo"))
                }
                if (project.hasProperty(produceCCSerializationError)) {
                    mappedTaskOutputProvider().get()
                }
            }

            assertContains(
                catchBuildFailures<InvalidUserCodeException>().buildAndReturn(
                    "tasks", "-P${produceCCSerializationError}",
                ).unwrap().single().message!!,
                "Querying the mapped value of",
            )

            providerBuildScriptReturn {
                mappedTaskOutputProvider()
            }.buildAndReturn(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
            )
        }
    }

    @GradleTest
    fun catchExceptions(version: GradleVersion) {
        data class A(val name: String = "A") : Exception()
        data class B(val name: String = "B") : Exception()

        val a1 = A("1")
        val a2 = A("2")

        // Catch exceptions emitted by tasks at execution
        project("empty", version) {
            buildScriptInjection {
                project.tasks.register("throwA1") {
                    it.doLast { throw a1 }
                }
                project.tasks.register("throwA2") {
                    it.doLast { throw a2 }
                }
                project.tasks.register("throwA") {
                    it.dependsOn("throwA1", "throwA2")
                }
                project.tasks.register("throwB") {
                    it.doLast { throw B() }
                }
                project.tasks.register("noBuildFailure") {}
            }
            assertEquals(
                CaughtBuildFailure.Expected(setOf(a1, a2)),
                catchBuildFailures<A>().buildAndReturn(
                    "throwA",
                    deriveBuildOptions = { defaultBuildOptions.copy(continueAfterFailure = true) }
                )
            )
            assert(
                assertIsInstance<CaughtBuildFailure.Unexpected<A>>(
                    catchBuildFailures<A>().buildAndReturn("throwB")
                ).stackTraceDump.contains("Caused by: B(name=B)")
            )
            assertIsInstance<UnexpectedBuildSuccess>(
                runCatching {
                    catchBuildFailures<A>().buildAndReturn("noBuildFailure")
                }.exceptionOrNull()
            )
        }

        // Build failures caused by configuration errors are also catchable
        project("empty", version) {
            buildScriptInjection {
                throw A()
            }
            assertEquals(
                CaughtBuildFailure.Expected(setOf(A())),
                catchBuildFailures<A>().buildAndReturn("tasks")
            )
        }

        project("empty", version) {
            buildScriptInjection {
                throw B()
            }
            assert(
                assertIsInstance<CaughtBuildFailure.Unexpected<A>>(
                    catchBuildFailures<A>().buildAndReturn("tasks")
                ).stackTraceDump.contains("Caused by: B(name=B)")
            )
        }
    }

    @GradleTest
    fun buildscriptBlockInjection(version: GradleVersion) {
        testBuildscriptBlockInjection(
            "emptyKts",
            version,
        )
    }

    @GradleTest
    fun buildscriptBlockInjectionGroovy(version: GradleVersion) {
        testBuildscriptBlockInjection(
            "empty",
            version,
        )
    }

    @GradleTestVersions(
        minVersion = TestVersions.Gradle.MAX_SUPPORTED,
    )
    @GradleTest
    fun testPrependToOrCreateBuildscriptBlock() {
        assertEquals(
            """
            buildscript {
            foo
            
            }
            
            """.trimIndent(),
            """
            buildscript {
            }
            
            """.trimIndent().prependToOrCreateBuildscriptBlock("foo")
        )
        assertEquals(
            """
            buildscript {
            foo
            }
            
            """.trimIndent(),
            """
            """.trimIndent().prependToOrCreateBuildscriptBlock("foo")
        )
    }

    private fun testBuildscriptBlockInjection(
        bareTemplate: String,
        version: GradleVersion,
    ) {
        // Bare template build script should not see KGP
        project(bareTemplate, version) {
            buildScriptInjection {
                project.plugins.apply("org.jetbrains.kotlin.multiplatform")
            }
            assertIsInstance<UnknownPluginException>(
                catchBuildFailures<UnknownPluginException>().buildAndReturn(
                    "help",
                ).unwrap().single()
            )
        }
        // But if we inject KGP everything should work
        project(bareTemplate, version) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.plugins.apply("org.jetbrains.kotlin.multiplatform")
            }
            build("help")
        }
    }

    private inline fun <reified T> assertIsInstance(value: Any?): T {
        if (value is T) return value
        fail("Expected $value to implement ${T::class.java}")
    }

    private fun publishAndConsumeProject(
        targetProject: String,
        version: GradleVersion,
    ) {
        val publishedProject = project(
            targetProject,
            version,
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    sourceSets.all {
                        it.compileSource("class ${it.name}")
                    }
                }
            }
        }.publish(PublisherConfiguration())

        project(
            targetProject,
            version,
        ) {
            addKgpToBuildScriptCompilationClasspath()
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
