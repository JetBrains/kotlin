/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.internal.GradleInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.util.GradleVersion
import org.gradle.api.internal.project.ProjectStateRegistry
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.testbase.SourceSetIdentifier
import org.jetbrains.kotlin.gradle.testbase.addIdentifierClass
import org.jetbrains.kotlin.gradle.artifacts.UklibResolutionStrategy
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.Serializable

@MppGradlePluginTests
@DisplayName("Smoke test uklib consumption")
class UklibConsumptionIT : KGPBaseTest() {

    @GradleTestVersions
    @GradleTest
    fun `uklib consumption - in kotlin compilations of a symmetric consumer and producer projects - with all metadata compilations`(
        version: GradleVersion,
        @TempDir tempDir: File,
    ) {
        val symmetricTargets: KotlinMultiplatformExtension.() -> Unit = @JvmSerializableLambda {
            linuxArm64()
            linuxX64()
            iosArm64()
            iosX64()
            jvm()
            js()
            wasmJs()
            wasmWasi()
        }
        val publisherProject = publishUklib(version, symmetricTargets)

        project(
            "buildScriptInjectionGroovy",
            version,
            dependencyManagement = DependencyManagement.DefaultDependencyManagement(
                gradleRepositoriesMode = RepositoriesMode.PREFER_PROJECT,
            )
        ) {
            transferDependencyResolutionRepositoriesIntoProjectRepositories()
            buildScriptInjection {
                project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_UKLIB_RESOLUTION_STRATEGY, UklibResolutionStrategy.AllowResolvingUklibs.propertyName)

                project.plugins.apply("org.jetbrains.kotlin.multiplatform")

                project.repositories.maven {
                    it.setUrl(publisherProject.repository)

                    // Prevent Gradle from reading Gradle metadata
                    it.metadataSources {
                        it.mavenPom()
                        it.ignoreGradleMetadataRedirection()
                    }
                }

                with(kotlinMultiplatform) {
                    symmetricTargets()

                    sourceSets.commonMain.dependencies {
                        implementation(publisherProject.coordinates)
                    }
                }
            }

            data class Return(
                val files: List<File>,
                val buildDirectory: File,
            ) : Serializable

            val linuxMainUklibResolution = buildScriptReturn {
                // FIXME: ???
                (project.gradle as GradleInternal).services.get(ProjectStateRegistry::class.java).allowUncontrolledAccessToAnyProject {
                    Return(
                        files = with(kotlinMultiplatform) {
                            metadata().compilations.getByName("linuxMain")
                                .compileDependencyFiles
                                .map { it }
                        },
                        buildDirectory = project.layout.buildDirectory.asFile.get()
                    )
                }
            }.buildAndReturn("compileLinuxMainKotlinMetadata")

            val out = linuxMainUklibResolution.files
                .filter { it.startsWith(linuxMainUklibResolution.buildDirectory) }
                .map {
                    it.relativeTo(linuxMainUklibResolution.buildDirectory)
                }

            println(out)
        }
    }

    data class PublisherProject(
        val repository: File,
        val coordinates: String,
    ) : Serializable

    private fun publishUklib(
        gradleVersion: GradleVersion,
        publisherConfiguration: KotlinMultiplatformExtension.() -> Unit,
    ): PublisherProject {
        val publisherGroup = "foo"
        val publisherVersion = "1.0"
        val publisherName = "producer"
        var publicationRepoPath: File? = null
        project(
            "buildScriptInjectionGroovy",
            gradleVersion,
            projectPathAdditionalSuffix = publisherName,
        ) {
            val publicationRepo: Project.() -> Directory = @JvmSerializableLambda{ project.layout.projectDirectory.dir("repo") }
            buildScriptInjection {
                project.propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_MPP_PUBLISH_UKLIB, true.toString())

                project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                project.plugins.apply("maven-publish")

                project.group = publisherGroup
                project.version = publisherVersion

                with(kotlinMultiplatform) {
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

            build("publishAllPublicationsToMavenRepository")

            publicationRepoPath = buildScriptReturn {
                project.publicationRepo().asFile
            }.buildAndReturn()
        }
        return PublisherProject(
            publicationRepoPath!!,
            "${publisherGroup}:${publisherName}:${publisherVersion}",
        )
    }
}

val Project.propertiesExtension: ExtraPropertiesExtension
    get() = extensions.getByType(ExtraPropertiesExtension::class.java)