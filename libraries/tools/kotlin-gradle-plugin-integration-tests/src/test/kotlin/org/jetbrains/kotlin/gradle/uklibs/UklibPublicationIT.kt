/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import com.android.build.api.dsl.LibraryExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.mpp.resources.unzip
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.ArchiveUklibTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.MavenModule
import org.jetbrains.kotlin.gradle.util.parsePom
import org.junit.jupiter.api.DisplayName
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class, ExperimentalWasmDsl::class)
@MppGradlePluginTests
@DisplayName("Smoke test uklib artifact publication")
class UklibPublicationIT : KGPBaseTest() {

    @GradleTest
    fun `uklib contents - produces expected umanifest, platform and metadata artifacts`(
        gradleVersion: GradleVersion,
    ) {
        val publisher = publishUklib(
            gradleVersion = gradleVersion,
        ) {
            linuxArm64()
            linuxX64()
            iosArm64()
            @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
            iosX64()
            jvm()
            js()
            wasmJs()
            wasmWasi()

            sourceSets.iosMain.get().compileStubSourceWithSourceSetName()
            sourceSets.linuxMain.get().compileStubSourceWithSourceSetName()
            sourceSets.nativeMain.get().compileStubSourceWithSourceSetName()
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
        }

        val expectedFragments = setOf(
            Fragment(
                identifier = "commonMain",
                targets = listOf("ios_arm64", "ios_x64", "js_ir", "jvm", "linux_arm64", "linux_x64", "wasm_js", "wasm_wasi")
            ),
            Fragment(identifier = "iosArm64Main", targets = listOf("ios_arm64")),
            Fragment(identifier = "iosMain", targets = listOf("ios_arm64", "ios_x64")),
            Fragment(identifier = "iosX64Main", targets = listOf("ios_x64")),
            Fragment(identifier = "jsMain", targets = listOf("js_ir")),
            Fragment(identifier = "jvmMain", targets = listOf("jvm")),
            Fragment(identifier = "linuxArm64Main", targets = listOf("linux_arm64")),
            Fragment(identifier = "linuxMain", targets = listOf("linux_arm64", "linux_x64")),
            Fragment(identifier = "linuxX64Main", targets = listOf("linux_x64")),
            Fragment(identifier = "nativeMain", targets = listOf("ios_arm64", "ios_x64", "linux_arm64", "linux_x64")),
            Fragment(identifier = "wasmJsMain", targets = listOf("wasm_js")),
            Fragment(identifier = "wasmWasiMain", targets = listOf("wasm_wasi")),
        )

        assertPublishedFragments(expectedFragments, publisher)
    }

    @GradleTest
    fun `uklib contents - produces single platform fragment - when metadata compilations are redundant`(
        gradleVersion: GradleVersion,
    ) {
        val publisher = publishUklib(
            gradleVersion = gradleVersion
        ) {
            iosArm64()
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
        }

        val expectedFragments = setOf(
            Fragment(identifier = "iosArm64Main", targets = listOf("ios_arm64")),
        )

        assertPublishedFragments(
            expectedFragments,
            publisher,
        )
    }

    @GradleTest
    fun `uklib contents - multiple targets with a single common source`(
        gradleVersion: GradleVersion,
    ) {
        val publishedProject = project(
            "empty",
            gradleVersion
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    iosArm64()
                    @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
                    iosX64()
                    sourceSets.commonMain.get().compileSource("class Common")
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration())

        val publishedUklib = readProducedUklib(publishedProject)

        val expectedFragments = setOf(
            Fragment(identifier = "iosArm64Main", targets = listOf("ios_arm64")),
            Fragment(identifier = "iosX64Main", targets = listOf("ios_x64")),
            Fragment(identifier = "commonMain", targets = listOf("ios_arm64", "ios_x64")),
        )

        assertPublishedFragments(
            expectedFragments,
            publishedUklib,
        )
    }

    @GradleTest
    fun `uklib contents - multiple targets with stale metadata compilation`(
        gradleVersion: GradleVersion,
    ) {
        val compileAppleMain = "compileAppleMain"
        val project = project(
            "empty",
            gradleVersion,
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    iosArm64()
                    @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
                    iosX64()

                    if (project.hasProperty(compileAppleMain)) {
                        sourceSets.appleMain.get().compileSource("class Apple")
                    } else {
                        sourceSets.commonMain.get().compileSource("class Common")
                    }
                }
            }
        }

        val publicationWithApple = project.publish(
            "-P${compileAppleMain}",
            publisherConfiguration = PublisherConfiguration(repoPath = "withApple"),
        )
        assertPublishedFragments(
            setOf(
                Fragment(identifier = "iosArm64Main", targets = listOf("ios_arm64")),
                Fragment(identifier = "iosX64Main", targets = listOf("ios_x64")),
                Fragment(identifier = "appleMain", targets = listOf("ios_arm64", "ios_x64")),
            ),
            readProducedUklib(publicationWithApple)
        )

        val incrementalPublicationWithoutApple = project.publish(
            publisherConfiguration = PublisherConfiguration(repoPath = "withoutApple"),
        )
        assertPublishedFragments(
            setOf(
                Fragment(identifier = "iosArm64Main", targets = listOf("ios_arm64")),
                Fragment(identifier = "iosX64Main", targets = listOf("ios_x64")),
                Fragment(identifier = "commonMain", targets = listOf("ios_arm64", "ios_x64")),
            ),
            readProducedUklib(incrementalPublicationWithoutApple)
        )
    }

    @GradleTest
    fun `uklib contents - publication with cinterops`(
        gradleVersion: GradleVersion,
    ) {
        val project = project(
            "empty",
            gradleVersion,
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
        }

        val publication = project.publish()
        assertPublishedFragments(
            setOf(
                Fragment(
                    identifier = "commonMain", targets = mutableListOf("linux_arm64", "linux_x64"),
                ),
                Fragment(
                    identifier = "linuxArm64Main", targets = mutableListOf("linux_arm64"),
                ),
                Fragment(
                    identifier = "linuxArm64Main_cinterop_bar", targets = mutableListOf("linux_arm64"),
                ),
                Fragment(
                    identifier = "linuxArm64Main_cinterop_foo", targets = mutableListOf("linux_arm64"),
                ),
                Fragment(
                    identifier = "linuxX64Main", targets = mutableListOf("linux_x64"),
                ),
                Fragment(
                    identifier = "linuxX64Main_cinterop_bar", targets = mutableListOf("linux_x64"),
                ),
                Fragment(
                    identifier = "linuxX64Main_cinterop_foo", targets = mutableListOf("linux_x64"),
                ),
            ),
            readProducedUklib(publication)
        )
    }

    @GradleTest
    fun `uklib contents - bamboo metadata publication`(
        gradleVersion: GradleVersion,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion
        ) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.setupMavenPublication("Stub", PublisherConfiguration())
                project.applyMultiplatform {
                    iosArm64()
                    @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
                    iosX64()

                    sourceSets.all {
                        it.compileStubSourceWithSourceSetName()
                    }
                }
                project.tasks.configureEach {
                    if (it is ArchiveUklibTask) {
                        it.checkForBamboosInUklib.set(true)
                    }
                }
            }

            val iosAttributes = setOf("ios_arm64", "ios_x64")
            assertEquals(
                ArchiveUklibTask.UklibWithDuplicateAttributes(
                    mapOf(
                        iosAttributes to setOf(
                            "iosMain",
                            "appleMain",
                            "nativeMain",
                            "commonMain"
                        )
                    ),
                ),
                catchBuildFailures<ArchiveUklibTask.UklibWithDuplicateAttributes>().buildAndReturn(
                    "publishAllPublicationsToStubRepository"
                ).unwrap().single(),
            )
        }
    }

    @GradleAndroidTest
    fun `uklib publication - with AGP`(
        gradleVersion: GradleVersion,
        agpVersion: String,
    ) {
        val publisher = publishUklib(
            template = "empty",
            gradleVersion = gradleVersion,
            androidVersion = agpVersion,
        ) {
            project.plugins.apply("com.android.library")
            iosArm64()
            @Suppress("DEPRECATION")
            androidTarget()

            sourceSets.all {
                it.compileStubSourceWithSourceSetName()
            }

            with(project.extensions.getByType(LibraryExtension::class.java)) {
                compileSdk = 23
                namespace = "kotlin.multiplatform.projects"
            }
        }

        val expectedFragments = setOf(
            Fragment(identifier = "commonMain", targets = listOf("android", "ios_arm64")),
            Fragment(identifier = "iosArm64Main", targets = listOf("ios_arm64")),
        )

        assertPublishedFragments(
            expectedFragments,
            publisher,
        )
    }

    private fun assertPublishedFragments(
        expectedFragments: Set<Fragment>,
        publisher: ProducedUklib,
    ) {
        assertEquals(
            Umanifest(expectedFragments).prettyPrinted,
            publisher.umanifest.prettyPrinted,
        )
        assertEquals(
            expectedFragments.map { it.identifier }.toSet().prettyPrinted,
            publisher.uklibContents.listDirectoryEntries().map {
                it.name
            }.filterNot { it == Uklib.UMANIFEST_FILE_NAME }.toSet().prettyPrinted,
        )
    }

    @GradleTest
    fun `uklib POM - publication with project dependencies`(
        gradleVersion: GradleVersion,
    ) {
        project(
            "empty",
            gradleVersion,
        ) {
            addKgpToBuildScriptCompilationClasspath()
            val dependency = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.setUklibPublicationStrategy()
                    project.setUklibResolutionStrategy()
                    project.plugins.apply("maven-publish")
                    project.group = "dependencyGroup"
                    project.version = "2.0"
                    project.applyMultiplatform {
                        linuxArm64()
                        linuxX64()
                        jvm()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                    }
                }
            }
            val publisher = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.setUklibPublicationStrategy()
                    project.setUklibResolutionStrategy()
                    project.applyMultiplatform {
                        linuxArm64()
                        linuxX64()
                        jvm()
                        sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                        sourceSets.commonMain.dependencies {
                            implementation(project(":dependency"))
                        }
                    }
                }
            }

            include(dependency, "dependency")
            include(publisher, "publisher")

            val repoId = "Publisher"

            val publishedProject = publisher.publishReturn(
                repositoryIdentifier = repoId
            ).buildAndReturn(
                ":publisher:publishAllPublicationsTo${repoId}Repository",
                "-P${repoId}",
                executingProject = this,
                deriveBuildOptions = {
                    defaultBuildOptions.copy(
                        // FIXME: PI doesn't work because POM rewriter for Uklib is not yet supported
                        isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
                    )
                }
            )
            assertEquals(
                listOf(
                    MavenModule(
                        groupId = "dependencyGroup",
                        artifactId = "dependency",
                        version = "2.0",
                        /**
                         * FIXME: This test is currently broken because POM rewriter for Uklibs is not yet ready. The scope is supposed to be "compile"
                         */
                        // scope = "compile"
                        scope = "runtime"
                    ),
                ),
                parsePom(publishedProject.rootComponent.pom).dependencies().filterNot {
                    it.artifactId == "kotlin-stdlib"
                },
            )
        }
    }

    @GradleTest
    fun `uklib POM - publication with dependencies in different scopes`(
        gradleVersion: GradleVersion,
    ) {
        val producerImplementation = publishUklib(
            gradleVersion = gradleVersion,
            publisherConfig = PublisherConfiguration(group = "implementation")
        ) {
            jvm()
            js()
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
        }.publishedProject

        val producerApi = publishUklib(
            gradleVersion = gradleVersion,
            publisherConfig = PublisherConfiguration(group = "api")
        ) {
            jvm()
            js()
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
        }.publishedProject

        val project = publishUklib(
            gradleVersion = gradleVersion,
            dependencyRepositories = listOf(producerImplementation, producerApi),
        ) {
            // FIXME: Allow consuming Uklibs
            jvm()
            js()
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
            sourceSets.commonMain.dependencies {
                implementation(producerImplementation.rootCoordinate)
                api(producerApi.rootCoordinate)
            }
        }

        val publisher = project.publishedProject
        assertEquals(
            listOf(
                MavenModule(
                    groupId = "api",
                    artifactId = "empty",
                    version = "1.0",
                    /**
                     * FIXME: This test is currently broken because POM rewriter for Uklibs is not yet ready. The scope is supposed to be "compile"
                     */
                    // scope = "compile"
                    scope = "runtime"
                ),
                MavenModule(groupId = "implementation", artifactId = "empty", version = "1.0", scope = "runtime"),
            ),
            parsePom(publisher.rootComponent.pom).dependencies().filterNot {
                it.artifactId in setOf(
                    "kotlin-stdlib",
                    "kotlin-dom-api-compat",
                )
            },
        )
    }

    @GradleTest
    fun `uklib POM - publication with dependencies in native targets - all dependencies are compile`(
        gradleVersion: GradleVersion,
    ) {
        val producer = publishUklib(
            gradleVersion = gradleVersion,
            publisherConfig = PublisherConfiguration(group = "dependency")
        ) {
            iosArm64()
            @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
            iosX64()
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
        }.publishedProject

        val project = publishUklib(
            gradleVersion = gradleVersion,
            dependencyRepositories = listOf(producer),
        ) {
            // FIXME: Enable uklib consumption?
            iosArm64()
            @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
            iosX64()
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
            sourceSets.commonMain.dependencies {
                implementation(producer.rootCoordinate)
            }
        }

        val publisher = project.publishedProject
        assertEquals(
            listOf(
                MavenModule(
                    groupId = "dependency",
                    artifactId = "empty",
                    version = "1.0",
                    /**
                     * FIXME: This test is currently broken because POM rewriter for Uklibs is not yet ready. The scope is supposed to be "compile"
                     */
                    // scope = "compile"
                    scope = "runtime"
                ),
            ),
            parsePom(publisher.rootComponent.pom).dependencies().filterNot {
                it.artifactId == "kotlin-stdlib"
            },
        )
    }

    @GradleTest
    fun `uklib POM - kotlin-dom-api-compat with pom type`(
        gradleVersion: GradleVersion,
    ) {
        val producer = publishUklib(
            gradleVersion = gradleVersion,
            publisherConfig = PublisherConfiguration(group = "dependency")
        ) {
            jvm()
            js()
            wasmJs()
            wasmWasi()
            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
        }.publishedProject

        assertEquals(
            listOf(
                MavenModule(
                    artifactId = "kotlin-dom-api-compat",
                    groupId = "org.jetbrains.kotlin",
                    scope = "runtime",
                    type = "pom",
                    version = KOTLIN_VERSION,
                ),
            ).prettyPrinted,
            parsePom(producer.rootComponent.pom).dependencies().filterNot {
                it.artifactId == "kotlin-stdlib"
            }.prettyPrinted,
        )
    }

    @GradleTest
    fun `uklib publication layout - with jvm target`(
        gradleVersion: GradleVersion,
    ) {
        val producer = publishUklib(
            gradleVersion = gradleVersion,
        ) {
            jvm()
            sourceSets.commonMain.get().compileSource("class Common")
        }.publishedProject

        val unpackedJar = producer.repository.resolve("jarContents").toPath()
        unzip(
            producer.rootComponent.jar.toPath(),
            unpackedJar,
            ""
        )

        assertFileExists(unpackedJar.resolve("Common.class"))
        // Make sure we don't collide with psm jar
        assertFileExists(producer.rootComponent.psmJar)
    }

    @GradleTest
    fun `uklib publication layout - without jvm target`(
        gradleVersion: GradleVersion,
    ) {
        val producer = publishUklib(
            gradleVersion = gradleVersion,
        ) {
            iosArm64()
            sourceSets.commonMain.get().compileSource("class Common")
        }.publishedProject

        assertFileExists(producer.rootComponent.jar)

        val unpackedJar = producer.repository.resolve("jarContents").toPath()
        unzip(
            producer.rootComponent.jar.toPath(),
            unpackedJar,
            ""
        )

        assertEquals(
            "META-INF",
            unpackedJar.listDirectoryEntries().single().name,
        )
    }

    @GradleTest
    fun `uklib assemble - without sources - doesn't fail build`(
        gradleVersion: GradleVersion,
    ) {
        project(
            "empty", gradleVersion
        ) {
            buildScriptInjection {
                project.setUklibPublicationStrategy()
                project.setUklibResolutionStrategy()
            }
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()
                    iosArm64()
                    @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
                    iosX64()
                }
            }
        }.build("assemble")
    }

    // FIXME: Test consumption of jvm variant of a Uklib publication in a java plugin consumer

    @kotlinx.serialization.Serializable
    data class Fragment(
        val identifier: String,
        val targets: List<String>,
    )

    @kotlinx.serialization.Serializable
    data class Umanifest(
        val fragments: Set<Fragment>,
        val manifestVersion: String = Uklib.CURRENT_UMANIFEST_VERSION,
    )

    data class ProducedUklib(
        val uklibContents: Path,
        val umanifest: Umanifest,
        val publishedProject: PublishedProject,
    ) : Serializable

    private fun publishUklib(
        template: String = "empty",
        gradleVersion: GradleVersion,
        androidVersion: String? = null,
        dependencyRepositories: List<PublishedProject> = emptyList(),
        publisherConfig: PublisherConfiguration = PublisherConfiguration(),
        configuration: KotlinMultiplatformExtension.() -> Unit,
    ): ProducedUklib {
        val publisher = project(
            template,
            gradleVersion,
        ) {
            if (androidVersion != null) addAgpToBuildScriptCompilationClasspath(androidVersion)
            addKgpToBuildScriptCompilationClasspath()
            dependencyRepositories.forEach(::addPublishedProjectToRepositories)
            buildScriptInjection {
                // FIXME: Enable cross compilation
                project.setUklibPublicationStrategy()
                project.applyMultiplatform {
                    configuration()
                }
            }
        }.publish(
            publisherConfiguration = publisherConfig,
            deriveBuildOptions = {
                defaultBuildOptions.copy(
                    androidVersion = androidVersion,
                    // WarningMode.None because of AGP issue: https://issuetracker.google.com/399393875
                    warningMode = if (androidVersion != null) WarningMode.None else defaultBuildOptions.warningMode,
                )
            }
        )

        return readProducedUklib(publisher)
    }

    private fun readProducedUklib(publisher: PublishedProject): ProducedUklib {
        val uklibPath = publisher.rootComponent.uklib.toPath()

        assertFileExists(uklibPath)

        val uklibContents = publisher.repository.resolve("uklibContents").toPath()
        uklibContents.createDirectory()
        unzip(
            uklibPath,
            uklibContents,
            ""
        )
        val umanifest = uklibContents.resolve("umanifest").inputStream().use {
            Json.decodeFromStream<Umanifest>(it)
        }
        return ProducedUklib(
            uklibContents = uklibContents,
            umanifest = umanifest,
            publishedProject = publisher
        )
    }
}
