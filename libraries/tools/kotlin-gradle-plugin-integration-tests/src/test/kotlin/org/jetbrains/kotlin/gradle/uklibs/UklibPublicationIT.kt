/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.mpp.resources.unzip
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.FileInputStream
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertEquals
import com.android.build.api.dsl.LibraryExtension
import kotlinx.serialization.ExperimentalSerializationApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.ArchiveUklibTask
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics.UklibFragmentsChecker
import org.jetbrains.kotlin.gradle.util.MavenModule
import org.jetbrains.kotlin.gradle.util.parsePom

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
            iosX64()
            jvm()
            js()
            wasmJs()
            wasmWasi()

            sourceSets.iosMain.get().addIdentifierClass()
            sourceSets.linuxMain.get().addIdentifierClass()
            sourceSets.nativeMain.get().addIdentifierClass()
            sourceSets.commonMain.get().addIdentifierClass()
        }

        val expectedFragments = setOf(
            // Fragment(identifier = "appleMain", targets = listOf("ios_arm64", "ios_x64")),
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

        assertEquals(
            Umanifest(expectedFragments),
            publisher.umanifest,
        )
        assertEquals(
            expectedFragments.map { it.identifier }.toSet(),
            publisher.uklibContents.listDirectoryEntries().map {
                it.name
            }.filterNot { it == Uklib.UMANIFEST_FILE_NAME }.toSet(),
        )
    }

    @GradleTest
    fun `uklib contents - produces single platform fragment - when metadata compilations are redundant`(
        gradleVersion: GradleVersion,
    ) {
        val publisher = publishUklib(
            gradleVersion = gradleVersion
        ) {
            iosArm64()
            sourceSets.commonMain.get().addIdentifierClass()
        }

        val expectedFragments = setOf(
            Fragment(identifier = "iosArm64Main", targets = listOf("ios_arm64")),
        )

        assertEquals(
            Umanifest(expectedFragments),
            publisher.umanifest,
        )
        assertEquals(
            expectedFragments.map { it.identifier }.toSet(),
            publisher.uklibContents.listDirectoryEntries().map {
                it.name
            }.filterNot { it == Uklib.UMANIFEST_FILE_NAME }.toSet(),
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
                project.enableUklibPublication()
                project.applyMultiplatform {
                    iosArm64()
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

        assertEquals(
            Umanifest(expectedFragments),
            publishedUklib.umanifest,
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
                project.enableUklibPublication()
                project.setupMavenPublication("Stub", PublisherConfiguration())
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()

                    sourceSets.all {
                        it.addIdentifierClass()
                    }
                }
            }

            val iosAttributes = setOf("ios_arm64", "ios_x64")
            assertEquals(
                ArchiveUklibTask.UklibWithDuplicateAttributes(
                    setOf(
                        UklibFragmentsChecker.Violation.DuplicateAttributesFragments(
                            attributes = iosAttributes,
                            duplicates = setOf(
                                UklibFragmentsChecker.FragmentToCheck(
                                    identifier = "iosMain",
                                    iosAttributes,
                                ),
                                UklibFragmentsChecker.FragmentToCheck(
                                    identifier = "appleMain",
                                    iosAttributes,
                                ),
                                UklibFragmentsChecker.FragmentToCheck(
                                    identifier = "nativeMain",
                                    iosAttributes,
                                ),
                                UklibFragmentsChecker.FragmentToCheck(
                                    identifier = "commonMain",
                                    iosAttributes,
                                ),
                            )
                        )
                    )
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
            androidTarget()

            sourceSets.all {
                it.addIdentifierClass()
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

        assertEquals(
            Umanifest(expectedFragments),
            publisher.umanifest,
        )
        assertEquals(
            expectedFragments.map { it.identifier }.toSet(),
            publisher.uklibContents.listDirectoryEntries().map {
                it.name
            }.filterNot { it == Uklib.UMANIFEST_FILE_NAME }.toSet(),
        )
    }

    // FIXME: jvm { withJava }

    @GradleTestVersions(
        minVersion = TestVersions.Gradle.MAX_SUPPORTED,
    )
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
//                    project.propertiesExtension.set(
//                        PropertiesProvider.PropertyNames.KOTLIN_KMP_ISOLATED_PROJECT_SUPPORT,
//                        KmpIsolatedProjectsSupport.ENABLE.name,
//                    )
                    project.group = "dependencyGroup"
                    project.version = "2.0"
                    project.applyMultiplatform {
                        linuxArm64()
                        linuxX64()
                        sourceSets.linuxMain.get().addIdentifierClass()
                    }
                }
            }
            val publisher = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.enableUklibPublication()
//                    project.
                    project.applyMultiplatform {
                        linuxArm64()
                        linuxX64()
                        sourceSets.commonMain.get().addIdentifierClass()
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
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                deriveBuildOptions = {
                    defaultBuildOptions.copy(
                        // FIXME: PI doesn't work
                        isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
                    )
                }
            )
            assertEquals(
                listOf(
                    MavenModule(groupId = "dependencyGroup", artifactId = "dependency", version = "2.0", scope = "compile"),
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
            sourceSets.commonMain.get().addIdentifierClass()
        }.publishedProject

        val producerApi = publishUklib(
            gradleVersion = gradleVersion,
            publisherConfig = PublisherConfiguration(group = "api")
        ) {
            jvm()
            js()
            sourceSets.commonMain.get().addIdentifierClass()
        }.publishedProject

        val project = publishUklib(
            gradleVersion = gradleVersion,
            dependencyRepositories = listOf(producerImplementation, producerApi),
        ) {
            // FIXME: Allow consuming Uklibs
            jvm()
            js()
            sourceSets.commonMain.get().addIdentifierClass()
            sourceSets.commonMain.dependencies {
                implementation(producerImplementation.rootCoordinate)
                api(producerApi.rootCoordinate)
            }
        }

        val publisher = project.publishedProject
        assertEquals(
            listOf(
                MavenModule(groupId = "api", artifactId = "empty", version = "1.0", scope = "compile"),
                MavenModule(groupId = "implementation", artifactId = "empty", version = "1.0", scope = "runtime"),
            ),
            parsePom(publisher.rootComponent.pom).dependencies().filterNot {
                it.artifactId == "kotlin-stdlib"
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
            iosX64()
            sourceSets.commonMain.get().addIdentifierClass()
        }.publishedProject

        val project = publishUklib(
            gradleVersion = gradleVersion,
            dependencyRepositories = listOf(producer),
        ) {
            // FIXME: Enable uklib consumption?
            iosArm64()
            iosX64()
            sourceSets.commonMain.get().addIdentifierClass()
            sourceSets.commonMain.dependencies {
                implementation(producer.rootCoordinate)
            }
        }

        val publisher = project.publishedProject
        assertEquals(
            listOf(
                MavenModule(groupId = "dependency", artifactId = "empty", version = "1.0", scope = "compile"),
            ),
            parsePom(publisher.rootComponent.pom).dependencies().filterNot {
                it.artifactId == "kotlin-stdlib"
            },
        )
    }

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
            // FIXME: Otherwise klib resolver explodes because of the name collision with self
//            projectPathAdditionalSuffix = publisherConfig.name,
        ) {
//            settingsBuildScriptInjection {
//                settings.rootProject.name = "publisher"
//            }
            if (androidVersion != null) addAgpToBuildScriptCompilationClasspath(androidVersion)
            addKgpToBuildScriptCompilationClasspath()
            dependencyRepositories.forEach(::addPublishedProjectToRepositories)
            buildScriptInjection {
                // FIXME: Enable cross compilation
                project.enableUklibPublication()
                project.applyMultiplatform {
                    configuration()
                }
            }
        }.publish(
            publisherConfiguration = publisherConfig,
            deriveBuildOptions = { defaultBuildOptions.copy(androidVersion = androidVersion) }
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

        return ProducedUklib(
            uklibContents = uklibContents,
            umanifest = Json.decodeFromStream<Umanifest>(
                FileInputStream(uklibContents.resolve("umanifest").toFile())
            ),
            publishedProject = publisher
        )
    }

}
