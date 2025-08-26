/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.JavaVersion
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.OutputFile
import org.gradle.api.plugins.UnknownPluginException
import org.gradle.kotlin.dsl.*
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.testkit.runner.UnexpectedBuildSuccess
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.uklibs.*
import org.jetbrains.kotlin.gradle.testbase.useAsZipFile
import org.jetbrains.kotlin.gradle.testing.PrettyPrint
import org.jetbrains.kotlin.gradle.testing.ResolvedComponentWithArtifacts
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.testing.resolveProjectDependencyComponentsWithArtifacts
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.test.*

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
    fun testMultipleInjections(version: GradleVersion) {
        project("empty", version) {
            val subproject = project("empty", version)
            include(subproject, "subproject")
            subproject.buildScriptInjection {
                println("subproject buildscript injection 1")
            }
            subProject("subproject").buildScriptInjection {
                println("subproject buildscript injection 2")
            }
            subProject("subproject").buildScriptInjection {
                println("subproject buildscript injection 3")
            }
            build(":subproject:help") {
                assertOutputContains("subproject buildscript injection 1")
                assertOutputContains("subproject buildscript injection 2")
                assertOutputContains("subproject buildscript injection 3")
            }
        }
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

    @GradleTest
    fun publishGeneratedJavaSource(version: GradleVersion) {
        project("empty", version) {
            buildScriptInjection {
                project.plugins.apply("java")
                java.sourceSets.getByName("main").compileJavaSource(
                    project,
                    className = "Generated",
                    """
                        public class Generated { }
                    """.trimIndent()
                )
            }

            assertEquals(
                setOf(
                    "META-INF/MANIFEST.MF",
                    "Generated.class",
                ),
                publishJava(PublisherConfiguration()).rootComponent.jar.useAsZipFile {
                    it.entries().asSequence().filter { !it.isDirectory }.map { it.name }.toSet()
                }
            )
        }
    }

    @DisplayName("Composite build")
    @GradleTest
    fun compositeBuild(version: GradleVersion) {
        val parent = "Parent"
        val child = "Child"
        val parentGroup = "foo"
        val parentId = "includeme"

        // Declare a Parent class in a project
        val parentClassProducer = project("empty", version) {
            settingsBuildScriptInjection {
                settings.rootProject.name = parentId
            }
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.group = parentGroup
                project.applyMultiplatform {
                    jvm()
                    sourceSets.getByName("commonMain").compileSource("open class ${parent}")
                }
            }
        }

        // Inherit from Parent in Child and consumer the project above as a modular dependency
        val consumer = project("empty", version) {
            includeBuild(parentClassProducer)
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    sourceSets.getByName("commonMain").compileSource("class ${child} : ${parent}()")
                    sourceSets.getByName("commonMain").dependencies {
                        implementation("${parentGroup}:${parentId}:1.0")
                    }
                }
            }
        }

        // Check we managed to compile the Child class
        assertFileExists(
            consumer.buildScriptReturn {
                kotlinMultiplatform.jvm()
                    .compilations
                    .getByName("main")
                    .output
                    .classesDirs
                    .files
                    .single { it.endsWith("kotlin/jvm/main") }
                    .resolve("${child}.class")
            }.buildAndReturn("compileKotlinJvm")
        )
    }

    @GradleTest
    fun kgpTestFixturesRuntime(version: GradleVersion) {
        // Check we have access to KGP testFixtures source set at compile and run time
        project("empty", version) {
            addKgpToBuildScriptCompilationClasspath()
            include(
                project("empty", version) {
                    buildScriptInjection {
                        val consumable = project.configurations.create("consumable") {
                            it.isCanBeResolved = false
                            it.attributes.attribute(
                                Usage.USAGE_ATTRIBUTE,
                                project.objects.named(Usage::class.java, Usage.JAVA_API)
                            )
                        }
                        project.artifacts.add(
                            consumable.name,
                            project.tasks.register("makeFoo", DefaultTask::class.java) {
                                val foo = project.layout.buildDirectory.file("foo.foo")
                                it.outputs.file(foo)
                                it.doLast { foo.get().asFile.createNewFile() }
                            },
                        )
                    }
                },
                "sub",
            )

            val resolvedConfiguration = providerBuildScriptReturn {
                val resolvable = project.configurations.create("resolvable") {
                    it.isCanBeConsumed = false
                    it.attributes.attribute(
                        Usage.USAGE_ATTRIBUTE,
                        project.objects.named(Usage::class.java, Usage.JAVA_API)
                    )
                    it.dependencies.add(project.dependencies.project(mapOf("path" to ":sub")))
                }
                project.provider {
                    project.ignoreAccessViolations {
                        resolvable.resolveProjectDependencyComponentsWithArtifacts()
                    }
                }
            }.buildAndReturn()

            assertEquals<PrettyPrint<Map<String, ResolvedComponentWithArtifacts>>>(
                mutableMapOf<String, ResolvedComponentWithArtifacts>(
                    ":sub" to ResolvedComponentWithArtifacts(
                        artifacts = mutableListOf(
                            mutableMapOf(
                                "artifactType" to "foo",
                                "org.gradle.usage" to "java-api",
                            ),
                        ),
                        configuration = "consumable",
                    ),
                ).prettyPrinted,
                resolvedConfiguration.prettyPrinted,
            )
        }
    }

    @GradleTest
    fun pluginApplicationSugar(version: GradleVersion) {
        val appliedExtension = "appliedExtension"
        val appliedPluginId = "com.example.applied"
        val notAppliedExtension = "notAppliedExtension"
        val notAppliedPluginId = "com.example.notApplied"
        val samplePlugin = project("empty", version) {
            plugins {
                kotlin("jvm")
                `java-gradle-plugin`
                `maven-publish`
            }
            buildScriptInjection {
                project.configurations.getByName("compileOnly").dependencies.add(
                    project.dependencies.gradleApi()
                )
                java.targetCompatibility = JavaVersion.VERSION_1_8
                kotlinJvm.compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
                kotlinJvm.sourceSets.getByName("main").compileSource(
                    """
                    interface Ext
                    class Applied : org.gradle.api.Plugin<org.gradle.api.Project> {
                        override fun apply(target: org.gradle.api.Project) {
                            target.extensions.add(Ext::class.java, "$appliedExtension", target.objects.newInstance(Ext::class.java))
                        }
                    }
                    
                    class NotApplied : org.gradle.api.Plugin<org.gradle.api.Project> {
                        override fun apply(target: org.gradle.api.Project) {
                            target.extensions.add(Ext::class.java, "$notAppliedExtension", target.objects.newInstance(Ext::class.java))
                        }
                    }
                    """.trimIndent()
                )

                project.extensions.getByType<GradlePluginDevelopmentExtension>().apply {
                    plugins.create("applied") {
                        it.id = appliedPluginId
                        it.implementationClass = "Applied"
                    }
                    plugins.create("notApplied") {
                        it.id = notAppliedPluginId
                        it.implementationClass = "NotApplied"
                    }
                }
            }
        }.publishJava(publisherConfiguration = PublisherConfiguration(group = "sample_plugin", version = "1.0"))

        project("empty", version) {
            settingsBuildScriptInjection {
                settings.pluginManagement.repositories.maven(samplePlugin.repository)
            }
            assertTrue(
                buildScriptReturn {
                    try {
                        this.javaClass.classLoader.loadClass(KotlinMultiplatformExtension::class.java.name)
                    } catch (e: NoClassDefFoundError) {
                        return@buildScriptReturn true
                    }
                    return@buildScriptReturn false
                }.buildAndReturn(),
                "Build script is not supposed to see KGP classes at this point",
            )
            plugins {
                kotlin("multiplatform")
                id(appliedPluginId) version "1.0"
                id(notAppliedPluginId) version "1.0" apply false
            }
            assertTrue(
                buildScriptReturn {
                    this.javaClass.classLoader.loadClass(KotlinMultiplatformExtension::class.java.name).isInstance(
                        project.extensions.getByName("kotlin")
                    )
                }.buildAndReturn(),
                "At this point the plugin is expected to be applied and the extension must inherit from the relevant class",
            )
            assertTrue(
                buildScriptReturn {
                    project.extensions.findByName(appliedExtension) != null
                }.buildAndReturn(),
                "Extension is expected to be registered at \"$appliedExtension\"",
            )
            assertTrue(
                buildScriptReturn {
                    project.extensions.findByName(notAppliedExtension) == null
                }.buildAndReturn(),
                "Extension is expected to not be registered at \"$notAppliedExtension\"",
            )
        }
    }

    @GradleAndroidTest
    fun pluginApplicationSugarAgpGroovy(
        version: GradleVersion,
        agpVersion: String,
    ) = testPluginApplicationSugarAgp("empty", version, agpVersion)

    @GradleAndroidTest
    fun pluginApplicationSugarAgp(
        version: GradleVersion,
        agpVersion: String,
    ) = testPluginApplicationSugarAgp("emptyKts", version, agpVersion)

    private fun testPluginApplicationSugarAgp(
        template: String,
        version: GradleVersion,
        agpVersion: String,
    ) {
        project(
            template,
            version,
            defaultBuildOptions.copy(androidVersion = agpVersion)
        ) {
            assertTrue(
                buildScriptReturn {
                    try {
                        this.javaClass.classLoader.loadClass(LibraryExtension::class.java.name)
                    } catch (e: NoClassDefFoundError) {
                        return@buildScriptReturn true
                    }
                    return@buildScriptReturn false
                }.buildAndReturn(),
                "Build script is not supposed to see AGP classes at this point",
            )
            plugins {
                id("com.android.library")
            }
            buildScriptInjection {
                with(project.extensions.getByType(LibraryExtension::class.java)) {
                    compileSdk = 23
                    namespace = "kotlin"
                }
            }
            assertTrue(
                buildScriptReturn {
                    this.javaClass.classLoader.loadClass(LibraryExtension::class.java.name).isInstance(
                        project.extensions.getByName("android")
                    )
                }.buildAndReturn(),
                "At this point the plugin is expected to be applied and the extension must inherit from the relevant class",
            )
        }
    }

    @Test
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
        val producerName = "producer"
        val publishedProject = project(
            targetProject,
            version,
        ) {
            settingsBuildScriptInjection {
                settings.rootProject.name = producerName
            }
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
        }.publish(publisherConfiguration = PublisherConfiguration())

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
                        implementation(publishedProject.rootCoordinate)
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

            val transformedFiles = providerBuildScriptReturn {
                with(kotlinMultiplatform) {
                    project.locateOrRegisterMetadataDependencyTransformationTask(
                        sourceSets.commonMain.get()
                    ).flatMap { it.allTransformedLibraries() }
                }
            }.buildAndReturn(metadataTransformationTaskName)

            assertEquals(
                listOf(
                    listOf("foo", producerName, "1.0", "linuxMain"),
                    listOf("foo", producerName, "1.0", "nativeMain"),
                    listOf("foo", producerName, "1.0", "commonMain"),
                ),
                transformedFiles.map { it.nameWithoutExtension.split("-").take(4) },
            )
        }
    }
}
