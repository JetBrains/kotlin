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
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics.UklibFragmentsChecker

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
            sourceSets.commonMain.get().addIdentifierClass()
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
            "buildScriptInjectionGroovy",
            gradleVersion
        ) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()
                    sourceSets.commonMain.get().compileSource("class Common")
                }
            }
        }.publish()

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
            "buildScriptInjectionGroovy",
            gradleVersion,
        ) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMultiplatform {
                    iosArm64()
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
    fun `uklib contents - bamboo metadata publication`(
        gradleVersion: GradleVersion,
    ) {
        project(
            "buildScriptInjectionGroovy",
            gradleVersion = gradleVersion
        ) {
            buildScriptInjection {
                project.enableUklibPublication()
                project.applyMavenPublish(PublisherConfiguration())
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
                    "publishAllPublicationsToMavenRepository"
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
            template = "buildScriptInjectionGroovyWithAGP",
            gradleVersion = gradleVersion,
            agpVersion = agpVersion,
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

    fun parsePom(file: File): Document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(file)

    fun Document.mavenDependencies(): List<MavenDependency> = documentElement
        .elements("dependencies").single()
        .elements("dependency")
        .map {
            MavenDependency(
                it.elements("groupId").single().textContent,
                it.elements("artifactId").single().textContent,
                it.elements("version").single().textContent,
                it.elements("scope").singleOrNull()?.textContent
            )
        }

    fun Element.elements(name: String): List<Element> {
        val elements = getElementsByTagName(name)
        return (0..<elements.length).map { elements.item(it) as Element }
    }

    data class MavenDependency(
        val groupId: String,
        val artifactId: String,
        val version: String,
        val scope: String?,
    )

    @GradleTestVersions(
        minVersion = TestVersions.Gradle.MAX_SUPPORTED,
    )
    @GradleTest
    fun `uklib POM - publication with project dependencies`(
        gradleVersion: GradleVersion,
    ) {
        project(
            "buildScriptInjectionGroovy",
            gradleVersion,
        ) {
            val dependency = project("buildScriptInjectionGroovy", gradleVersion) {
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
            val publisher = project("buildScriptInjectionGroovy", gradleVersion) {
                buildScriptInjection {
                    project.enableUklibPublication()
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

            val publishedProject = publisher.publishReturn(PublisherConfiguration()).buildAndReturn(
                ":publisher:publishAllPublicationsToMavenRepository",
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
                    MavenDependency(groupId = "dependencyGroup", artifactId = "dependency", version = "2.0", scope = "compile"),
                ),
                parsePom(publishedProject.pom).mavenDependencies().filterNot {
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
            publisherConfig = PublisherConfiguration(name = "implementation")
        ) {
            jvm()
            js()
            sourceSets.commonMain.get().addIdentifierClass()
        }.publishedProject

        val producerApi = publishUklib(
            gradleVersion = gradleVersion,
            publisherConfig = PublisherConfiguration(name = "api")
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
                implementation(producerImplementation.coordinate)
                api(producerApi.coordinate)
            }
        }

        val publisher = project.publishedProject
        assertEquals(
            listOf(
                MavenDependency(groupId = "foo", artifactId = "api", version = "1.0", scope = "compile"),
                MavenDependency(groupId = "foo", artifactId = "implementation", version = "1.0", scope = "runtime"),
            ),
            parsePom(publisher.pom).mavenDependencies().filterNot {
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
            publisherConfig = PublisherConfiguration(name = "dependency")
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
                implementation(producer.coordinate)
            }
        }

        val publisher = project.publishedProject
        assertEquals(
            listOf(
                MavenDependency(groupId = "foo", artifactId = "dependency", version = "1.0", scope = "compile"),
            ),
            parsePom(publisher.pom).mavenDependencies().filterNot {
                it.artifactId == "kotlin-stdlib"
            },
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
            producer.jar.toPath(),
            unpackedJar,
            ""
        )

        assertFileExists(unpackedJar.resolve("Common.class"))
        // Make sure we don't collide with psm jar
        assertFileExists(producer.psmJar)
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

        assertFileExists(producer.jar)

        val unpackedJar = producer.repository.resolve("jarContents").toPath()
        unzip(
            producer.jar.toPath(),
            unpackedJar,
            ""
        )

        assertEquals(
            "META-INF",
            unpackedJar.listDirectoryEntries().single().name,
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
        template: String = "buildScriptInjectionGroovy",
        gradleVersion: GradleVersion,
        agpVersion: String? = null,
        dependencyRepositories: List<PublishedProject> = emptyList(),
        publisherConfig: PublisherConfiguration = PublisherConfiguration(),
        configuration: KotlinMultiplatformExtension.() -> Unit,
    ): ProducedUklib {
        val publisher = project(
            template,
            gradleVersion,
            // FIXME: Otherwise klib resolver explodes because of the name collision with self
            projectPathAdditionalSuffix = publisherConfig.name,
        ) {
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
            deriveBuildOptions = { defaultBuildOptions.copy(androidVersion = agpVersion) }
        )

        return readProducedUklib(publisher)
    }

    private fun readProducedUklib(publisher: PublishedProject): ProducedUklib {
        val uklibPath = publisher.uklib.toPath()

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