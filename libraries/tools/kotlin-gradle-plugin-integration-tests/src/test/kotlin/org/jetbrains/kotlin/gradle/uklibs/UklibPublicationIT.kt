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
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.FileInputStream
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertEquals
import com.android.build.gradle.BaseExtension
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.mpp.external.*
import org.junit.jupiter.api.assertThrows
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.lang.Exception
import kotlin.test.assertContains

@OptIn(ExternalKotlinTargetApi::class)
@MppGradlePluginTests
@DisplayName("Smoke test uklib artifact publication")
class UklibPublicationIT : KGPBaseTest() {

    @GradleTest
    fun `uklib contents - produces expected umanifest, platform and metadata artifacts`(
        gradleVersion: GradleVersion
    ) {
        val publisher = publishUklib(
            gradleVersion = gradleVersion,
        )  {
            linuxArm64()
            linuxX64()
            iosArm64()
            iosX64()
            jvm()
            js()
            wasmJs()
            wasmWasi()
        }

        val expectedFragments = listOf(
            Fragment(identifier="appleMain", targets=listOf("ios_arm64", "ios_x64")),
            Fragment(identifier="commonMain", targets=listOf("ios_arm64", "ios_x64", "js_ir", "jvm", "linux_arm64", "linux_x64", "wasm_js", "wasm_wasi")),
            Fragment(identifier="iosArm64Main", targets=listOf("ios_arm64")),
            Fragment(identifier="iosMain", targets=listOf("ios_arm64", "ios_x64")),
            Fragment(identifier="iosX64Main", targets=listOf("ios_x64")),
            Fragment(identifier="jsMain", targets=listOf("js_ir")),
            Fragment(identifier="jvmMain", targets=listOf("jvm")),
            Fragment(identifier="linuxArm64Main", targets=listOf("linux_arm64")),
            Fragment(identifier="linuxMain", targets=listOf("linux_arm64", "linux_x64")),
            Fragment(identifier="linuxX64Main", targets=listOf("linux_x64")),
            Fragment(identifier="nativeMain", targets=listOf("ios_arm64", "ios_x64", "linux_arm64", "linux_x64")),
            Fragment(identifier="wasmJsMain", targets=listOf("wasm_js")),
            Fragment(identifier="wasmWasiMain", targets=listOf("wasm_wasi")),
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
        )  {
            iosArm64()
        }

        val expectedFragments = listOf(
            Fragment(identifier="iosArm64Main", targets=listOf("ios_arm64")),
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

    // FIXME: This should be an error or we need to introduce refines edges
    @GradleTest
    fun `uklib contents - bamboo metadata publication`(
        gradleVersion: GradleVersion
    ) {
        val publisher = publishUklib(
            gradleVersion = gradleVersion
        )  {
            iosArm64()
            iosX64()
        }

        val expectedFragments = listOf(
            Fragment(identifier="appleMain", targets=listOf("ios_arm64", "ios_x64")),
            Fragment(identifier="commonMain", targets=listOf("ios_arm64", "ios_x64")),
            Fragment(identifier="iosArm64Main", targets=listOf("ios_arm64")),
            Fragment(identifier="iosMain", targets=listOf("ios_arm64", "ios_x64")),
            Fragment(identifier="iosX64Main", targets=listOf("ios_x64")),
            Fragment(identifier="nativeMain", targets=listOf("ios_arm64", "ios_x64")),
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

    @GradleAndroidTest
    fun `uklib publication - with AGP`(
        gradleVersion: GradleVersion,
        agpVersion: String,
    ) {
        val publisher = publishUklib(
            template = "buildScriptInjectionGroovyWithAGP",
            gradleVersion = gradleVersion,
            agpVersion = agpVersion,
        )  {
            project.plugins.apply("com.android.library")
            iosArm64()
            androidTarget()

            with(project.extensions.getByType(BaseExtension::class.java)) {
                compileSdkVersion(23)
                namespace = "kotlin.multiplatform.projects"
            }
        }

        val expectedFragments = listOf(
            Fragment(identifier="commonMain", targets=listOf("android", "ios_arm64")),
            Fragment(identifier="iosArm64Main", targets=listOf("ios_arm64")),
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

    // FIXME: Lift this to FT
    @GradleTest
    fun `uklib publication - with externalTarget`(
        gradleVersion: GradleVersion
    ) {
        project(
            "buildScriptInjectionGroovy",
            gradleVersion,
        ) {
            buildScriptInjection {
                // FIXME: Enable cross compilation
                project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_PUBLISH_UKLIB, true.toString())
                project.applyMultiplatform {
                    iosArm64()
                    iosX64()
                    val kotlin = this
                    createExternalKotlinTarget {
                        defaults()
                    }.createCompilation { defaults(kotlin) }

                    sourceSets.all {
                        it.addIdentifierClass()
                    }
                }
            }

            assertContains(
                assertThrows<Exception> {
                    publish(PublisherConfiguration())
                }.message!!,
                "FIXME: This is explicitly unsupported",
            )
        }
    }

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
        val scope: String?
    )

    @GradleTestVersions(
        minVersion = TestVersions.Gradle.MAX_SUPPORTED,
    )
    @GradleTest
    fun `uklib POM - publication with project dependencies`(
        gradleVersion: GradleVersion
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
                        sourceSets.all { it.addIdentifierClass() }
                    }
                }
            }
            val publisher = project("buildScriptInjectionGroovy", gradleVersion) {
                buildScriptInjection {
                    project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_PUBLISH_UKLIB, true.toString())
                    project.applyMultiplatform {
                        linuxArm64()
                        linuxX64()
                        sourceSets.all { it.addIdentifierClass() }
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
        gradleVersion: GradleVersion
    ) {
        val producerImplementation = publishUklib(
            gradleVersion = gradleVersion,
            publisherConfig = PublisherConfiguration(name = "implementation")
        ) {
            jvm()
            js()
        }.publishedProject

        val producerApi = publishUklib(
            gradleVersion = gradleVersion,
            publisherConfig = PublisherConfiguration(name = "api")
        ) {
            jvm()
            js()
        }.publishedProject

        val project = publishUklib(
            gradleVersion = gradleVersion,
            dependencyRepositories = listOf(producerImplementation, producerApi),
        ) {
            // FIXME: Allow consuming Uklibs
            jvm()
            js()
            sourceSets.commonMain.dependencies {
                implementation(
                    producerImplementation.coordinate
                )
                api(
                    producerApi.coordinate
                )
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
        gradleVersion: GradleVersion
    ) {
        val producer = publishUklib(
            gradleVersion = gradleVersion,
            publisherConfig = PublisherConfiguration(name = "dependency")
        ) {
            iosArm64()
            iosX64()
        }.publishedProject

        val project = publishUklib(
            gradleVersion = gradleVersion,
            dependencyRepositories = listOf(producer),
        ) {
            // FIXME: Enable uklib consumption?
            iosArm64()
            iosX64()
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
    fun `test`(
        gradleVersion: GradleVersion
    ) {
        val res = runTestProject("buildScriptInjectionGroovy", gradleVersion) {
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    macosArm64()
                    macosX64()
                    sourceSets.all { it.addIdentifierClass() }
                    applyDefaultHierarchyTemplate {
                        group("a") {
                            withLinuxArm64()
                            withLinuxX64()
                        }
                        group("b") {
                            withMacosArm64()
                            withMacosX64()
                        }
                    }
                }
            }
            publish(PublisherConfiguration(name = "transitive"))
        }.result
        println(res)
    }

    @kotlinx.serialization.Serializable
    data class Fragment(
        val identifier: String,
        val targets: List<String>,
    )
    @kotlinx.serialization.Serializable
    data class Umanifest(
        val fragments: List<Fragment>,
        val manifestVersion: String = Uklib.CURRENT_UMANIFEST_VERSION,
    )

    data class UklibProducer(
        val uklibContents: Path,
        val umanifest: Umanifest,
        val publishedProject: PublishedProject
    ) : Serializable

    private fun publishUklib(
        template: String = "buildScriptInjectionGroovy",
        gradleVersion: GradleVersion,
        agpVersion: String? = null,
        dependencyRepositories: List<PublishedProject> = emptyList(),
        publisherConfig: PublisherConfiguration = PublisherConfiguration(),
        configuration: KotlinMultiplatformExtension.() -> Unit,
    ): UklibProducer {
        val publisher = runTestProject(
            template,
            gradleVersion,
            // FIXME: Otherwise klib resolver explodes
            projectPathAdditionalSuffix = publisherConfig.name,
        ) {
            // FIXME: addPublishedProjectToRepositoriesAndIgnoreGradleMetadata?
            dependencyRepositories.forEach(::addPublishedProjectToRepositories)
            buildScriptInjection {
                // FIXME: Enable cross compilation
                project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_PUBLISH_UKLIB, true.toString())
                project.applyMultiplatform {
                    configuration()
                    sourceSets.all {
                        it.addIdentifierClass()
                    }
                }
            }

            publish(
                publisherConfig,
                deriveBuildOptions = { defaultBuildOptions.copy(androidVersion = agpVersion) }
            )
        }.result

        val uklibPath = publisher.uklib.toPath()

        assertFileExists(uklibPath)

        val uklibContents = publisher.repository.resolve("uklibContents").toPath()
        uklibContents.createDirectory()
        unzip(
            uklibPath,
            uklibContents,
            ""
        )

        return UklibProducer(
            uklibContents = uklibContents,
            umanifest = Json.decodeFromStream<Umanifest>(
                FileInputStream(uklibContents.resolve("umanifest").toFile())
            ),
            publishedProject = publisher
        )
    }

}