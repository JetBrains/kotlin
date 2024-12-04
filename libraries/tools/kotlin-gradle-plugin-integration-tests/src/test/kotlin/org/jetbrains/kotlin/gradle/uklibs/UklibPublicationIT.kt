/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.publish.PublishingExtension
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.mpp.resources.unzip
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.*
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.io.FileInputStream
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertEquals
import com.android.build.gradle.BaseExtension
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.external.*

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
        ) @JvmSerializableLambda {
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
        ) @JvmSerializableLambda {
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
        ) @JvmSerializableLambda {
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
        ) @JvmSerializableLambda {
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
            val publicationRepo: Project.() -> Directory = @JvmSerializableLambda{ project.layout.projectDirectory.dir("repo") }
            buildScriptInjection {
                // FIXME: Enable cross compilation
                project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_PUBLISH_UKLIB, true.toString())

                project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                project.plugins.apply("maven-publish")

                project.group = "foo"
                project.version = "1.0"

                with(kotlinMultiplatform) {
                    class FakeCompilation(delegate: Delegate) : DecoratedExternalKotlinCompilation(delegate) {
                        @Suppress("UNCHECKED_CAST", "DEPRECATION")
                        override val compilerOptions: HasCompilerOptions<KotlinJvmCompilerOptions>
                            get() = super.compilerOptions as HasCompilerOptions<KotlinJvmCompilerOptions>
                    }

                    class FakeTarget(delegate: Delegate) : DecoratedExternalKotlinTarget(delegate),
                        HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions> {

                        @Suppress("UNCHECKED_CAST")
                        override val compilations: NamedDomainObjectContainer<FakeCompilation>
                            get() = super.compilations as NamedDomainObjectContainer<FakeCompilation>

                        override val compilerOptions: KotlinJvmCompilerOptions
                            get() = super.compilerOptions as KotlinJvmCompilerOptions
                    }

                    fun ExternalKotlinTargetDescriptorBuilder<FakeTarget>.defaults() {
                        targetName = "fake"
                        platformType = KotlinPlatformType.jvm
                        targetFactory = ExternalKotlinTargetDescriptor.TargetFactory(::FakeTarget)
                    }

                    fun ExternalKotlinCompilationDescriptorBuilder<FakeCompilation>.defaults(
                        kotlin: KotlinMultiplatformExtension,
                        name: String = KotlinCompilation.MAIN_COMPILATION_NAME,
                    ) {
                        compilationName = name
                        compilationFactory = ExternalKotlinCompilationDescriptor.CompilationFactory(::FakeCompilation)
                        defaultSourceSet = kotlin.sourceSets.maybeCreate(name)
                    }

                    iosArm64()
                    iosX64()
                    val kotlin = this
                    createExternalKotlinTarget {
                        defaults()
                    }.createCompilation { defaults(kotlin) }

                    sourceSets.all {
                        it.addIdentifierClass(SourceSetIdentifier(it.name))
                    }
                }

                val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
                publishingExtension.repositories.maven {
                    it.url = project.uri(project.publicationRepo())
                }
            }

            buildAndFail("publishAllPublicationsToMavenRepository") {
                assertOutputContains("FIXME: This is explicitly unsupported")
            }
        }
    }

    @kotlinx.serialization.Serializable
    data class Fragment(
        val identifier: String,
        val targets: List<String>,
    )
    @kotlinx.serialization.Serializable
    data class Umanifest(
        val fragments: List<Fragment>,
    )

    data class UklibProducer(
        val uklibContents: Path,
        val umanifest: Umanifest,
    ) : Serializable

    private fun publishUklib(
        template: String = "buildScriptInjectionGroovy",
        gradleVersion: GradleVersion,
        agpVersion: String? = null,
        publisherConfiguration: KotlinMultiplatformExtension.() -> Unit,
    ): UklibProducer {
        val publisherGroup = "foo"
        val publisherVersion = "1.0"
        val publisherName = "producer"
        var repository: File? = null
        project(
            template,
            gradleVersion,
            projectPathAdditionalSuffix = publisherName,
        ) {
            val publicationRepo: Project.() -> Directory = @JvmSerializableLambda{ project.layout.projectDirectory.dir("repo") }
            buildScriptInjection {
                // FIXME: Enable cross compilation
                project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_PUBLISH_UKLIB, true.toString())

                project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                project.plugins.apply("maven-publish")

                project.group = publisherGroup
                project.version = publisherVersion

                with(kotlinMultiplatform) {
                    // Add a source to all source sets
                    publisherConfiguration()

                    sourceSets.all {
                        it.addIdentifierClass(SourceSetIdentifier(it.name))
                    }
                }

                val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
                publishingExtension.repositories.maven {
                    it.url = project.uri(project.publicationRepo())
                }
            }

            build(
                "publishAllPublicationsToMavenRepository",
                buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            )

            repository = buildScriptReturn {
                project.publicationRepo().asFile
            }.buildAndReturn(
                deriveBuildOptions = { defaultBuildOptions.copy(androidVersion = agpVersion) }
            )
        }

        val uklibPath = repository!!
            .resolve(publisherGroup).resolve(publisherName).resolve(publisherVersion)
            .resolve("${publisherName}-${publisherVersion}.uklib").toPath()

        assertFileExists(uklibPath)

        val uklibContents = repository!!.resolve("uklibContents").toPath()
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
            )
        )
    }

}