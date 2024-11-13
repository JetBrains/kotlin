/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.publish.PublishingExtension
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.File
import kotlin.test.assertEquals

@MppGradlePluginTests
class BuildScriptInjectionIT : KGPBaseTest() {

    @GradleTestVersions
    @GradleTest
    fun test(version: GradleVersion) {
        runTest(
            "buildScriptInjection",
            version,
        )
    }

    @GradleTestVersions
    @GradleTest
    fun testGroovy(version: GradleVersion) {
        runTest(
            "buildScriptInjectionGroovy",
            version,
        )
    }

    private fun runTest(
        targetProject: String,
        version: GradleVersion,
    ) {
        val publisherName = "producer"
        project(
            targetProject,
            version,
            projectPathAdditionalSuffix = publisherName,
        ) {
            val publisherGroup = "foo"
            val publisherVersion = "1.0"
            val publicationRepo: Project.() -> Directory = @JvmSerializableLambda{ project.layout.projectDirectory.dir("repo") }
            buildScriptInjection {
                project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                project.plugins.apply("maven-publish")

                project.group = publisherGroup
                project.version = publisherVersion

                with(kotlinMultiplatform) {
                    linuxArm64()
                    linuxX64()
                }

                val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
                publishingExtension.repositories.maven {
                    it.url = project.uri(project.publicationRepo())
                }
            }

            build("publishAllPublicationsToMavenRepository")

            val publicationRepoPath: File = buildScriptReturn {
                project.publicationRepo().asFile
            }.buildAndReturn()

            project(
                targetProject,
                version,
                dependencyManagement = DependencyManagement.DefaultDependencyManagement(
                    gradleRepositoriesMode = RepositoriesMode.PREFER_PROJECT,
                )
            ) {
                transferDependencyResolutionRepositoriesIntoProjectRepositories()
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    project.plugins.apply("maven-publish")

                    project.repositories.maven {
                        it.setUrl(publicationRepoPath)
                    }

                    with(kotlinMultiplatform) {
                        linuxArm64()
                        linuxX64()

                        sourceSets.commonMain.dependencies {
                            implementation("${publisherGroup}:${publisherName}:${publisherVersion}")
                        }
                    }
                }
                val metadataTaskName = buildScriptReturn {
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
                }.buildAndReturn(metadataTaskName)

                assertEquals(
                    listOf(
                        listOf("foo", "producer", "1.0", "linuxMain"),
                        listOf("foo", "producer", "1.0", "commonMain"),
                    ),
                    transformedFiles.map { it.nameWithoutExtension.split("-").dropLast(1) },
                )
            }
        }
    }
}