/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testbase.*
import java.io.File
import java.io.Serializable

fun Project.applyMultiplatform(
    configure: KotlinMultiplatformExtension.() -> Unit,
) {
    plugins.apply("org.jetbrains.kotlin.multiplatform")
    (extensions.getByName("kotlin") as KotlinMultiplatformExtension).configure()
}

data class PublisherConfiguration(
    val group: String = "foo",
    val name: String = "producer",
    val version: String = "1.0",
    val repoPath: String = "repo",
) : Serializable

data class PublishedProject(
    val repository: File,
    val group: String,
    val name: String,
    val version: String,
) : Serializable {
    val coordinate: String = "$group:$name:$version"

    val rootComponent: File get() = repository.resolve(group).resolve(name).resolve(version)
    val pom: File get() = rootComponent.resolve("${name}-${version}.pom")
    val uklib: File get() = rootComponent.resolve("${name}-${version}.uklib")
}

fun Project.applyMavenPublish(
    publisherConfiguration: PublisherConfiguration,
) {
    plugins.apply("maven-publish")

    group = publisherConfiguration.group
    version = publisherConfiguration.version

    val publishingExtension = extensions.getByType(PublishingExtension::class.java)
    publishingExtension.repositories.maven {
        it.url = uri(
            layout.projectDirectory.dir(publisherConfiguration.repoPath)
        )
    }

    publishingExtension.publications.withType(MavenPublication::class.java).configureEach {
        // FIXME: ???
        val components = it.artifactId.split("-", limit = 2)
        if (components.size > 1) {
            it.artifactId = "${publisherConfiguration.name}-${components[1]}"
        } else {
            it.artifactId = publisherConfiguration.name
        }
    }
}

fun TestProject.publish(
    publisherConfiguration: PublisherConfiguration,
    deriveBuildOptions: TestProject.() -> BuildOptions = { buildOptions },
): PublishedProject {
    buildScriptInjection {
        project.applyMavenPublish(publisherConfiguration)
    }

    return buildScriptReturn {
        PublishedProject(
            project.layout.projectDirectory.dir(publisherConfiguration.repoPath).asFile,
            publisherConfiguration.group,
            publisherConfiguration.name,
            publisherConfiguration.version,
        )
    }.buildAndReturn(
        "publishAllPublicationsToMavenRepository",
        deriveBuildOptions,
    )
}

fun TestProject.addPublishedProjectToRepositories(
    publishedProject: PublishedProject,
    configuration: MavenArtifactRepository.() -> Unit = {},
) {
    // FIXME: All of this implicitly relies on RepositoriesMode.PREFER_PROJECT
    transferDependencyResolutionRepositoriesIntoProjectRepositories()
    buildScriptInjection {
        project.repositories.maven {
            it.url = project.uri(publishedProject.repository)
            it.configuration()
        }
    }
}