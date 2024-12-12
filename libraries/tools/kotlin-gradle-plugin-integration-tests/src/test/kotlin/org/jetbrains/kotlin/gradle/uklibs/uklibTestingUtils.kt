/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.internal.compilerRunner.native.nativeCompilerClasspath
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.io.Serializable
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.*

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
        val components = it.artifactId.split("-", limit = 2)
        if (components.size > 1) {
            it.artifactId = "${publisherConfiguration.name}-${components[1]}"
        } else {
            it.artifactId = publisherConfiguration.name
        }
    }
}

fun TestProject.publishReturn(
    publisherConfiguration: PublisherConfiguration,
): ReturnFromBuildScriptAfterExecution<PublishedProject> {
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
    }
}

fun TestProject.publishJavaReturn(
    publisherConfiguration: PublisherConfiguration,
): ReturnFromBuildScriptAfterExecution<PublishedProject> {
    buildScriptInjection {
        project.applyMavenPublish(publisherConfiguration)
        val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)

        publishingExtension.publications.create("java", MavenPublication::class.java) {
            it.from(project.components.getByName("java"))
        }
    }

    return buildScriptReturn {
        PublishedProject(
            project.layout.projectDirectory.dir(publisherConfiguration.repoPath).asFile,
            publisherConfiguration.group,
            publisherConfiguration.name,
            publisherConfiguration.version,
        )
    }
}

fun TestProject.publish(
    publisherConfiguration: PublisherConfiguration,
    deriveBuildOptions: TestProject.() -> BuildOptions = { buildOptions },
): PublishedProject = publishReturn(publisherConfiguration).buildAndReturn(
    "publishAllPublicationsToMavenRepository",
    deriveBuildOptions = deriveBuildOptions,
)

fun TestProject.publishJava(
    publisherConfiguration: PublisherConfiguration,
    deriveBuildOptions: TestProject.() -> BuildOptions = { buildOptions },
): PublishedProject = publishJavaReturn(publisherConfiguration).buildAndReturn(
    "publishAllPublicationsToMavenRepository",
    deriveBuildOptions = deriveBuildOptions,
)

private const val transferDependencyResolutionRepositoriesIntoProjectRepositories = "transferDependencyResolutionRepositoriesIntoProjectRepositories"
fun TestProject.addPublishedProjectToRepositories(
    publishedProject: PublishedProject,
    configuration: MavenArtifactRepository.() -> Unit = {},
) {
    settingsBuildScriptInjection {
        settings.dependencyResolutionManagement.repositories.maven {
            it.url = publishedProject.repository.toURI()
            it.configuration()
        }
        if (!settings.extraProperties.has(transferDependencyResolutionRepositoriesIntoProjectRepositories)) {
            settings.extraProperties.set(transferDependencyResolutionRepositoriesIntoProjectRepositories, true)
            // Transfer dependencyResolutionManagement into project for compatibility with Gradle <8.1 because we emit repositories in the
            // build script there
            settings.gradle.beforeProject { project ->
                settings.dependencyResolutionManagement.repositories.all { rep ->
                    project.repositories.add(rep)
                }
            }
        }
    }
}

fun TestProject.include(
    subproject: TestProject,
    name: String,
) {
    Files.createSymbolicLink(projectPath.resolve(name), subproject.projectPath)
    settingsBuildScriptInjection {
        settings.include(":${name}")
    }
}

fun TestProject.dumpKlibMetadataSignatures(klib: File): String {
    val dumpName = "dump_${UUID.randomUUID().toString().replace("-", "_")}"
    val outputFile = projectPath.resolve(dumpName).toFile()
    outputFile.createNewFile()

    buildScriptInjection {
        project.tasks.register(dumpName) {
            val nativeCompilerClasspath = project.objects.nativeCompilerClasspath(
                project.nativeProperties.actualNativeHomeDirectory,
                project.nativeProperties.shouldUseEmbeddableCompilerJar,
            )
            it.inputs.files(nativeCompilerClasspath)
            it.doLast {
                URLClassLoader(
                    nativeCompilerClasspath.map { it.toURI().toURL() }.toTypedArray(),
                    /**
                     * KGP-IT running in Kotlin under debugger load in the same java process as the Kotlin Gradle daemon. For some reason we
                     * have KotlinNativePaths loaded in application classloader which fails a check in [defaultHomePath]
                     */
                    null,
                ).use { classLoader ->
                    val entryPoint = Class.forName("org.jetbrains.kotlin.cli.klib.Main", true, classLoader)
                        .declaredMethods
                        .single { it.name == "exec" }

                    val result = entryPoint.invoke(
                        null,
                        PrintStream(FileOutputStream(outputFile)),
                        System.err,
                        arrayOf(
                            "dump-metadata-signatures", klib.path,
                            "-test-mode", "true",
                        )
                    ) as Int
                    if (result != 0) {
                        error("Couldn't dump metadata klib at ${klib}. Stdout:\n${outputFile.readText()}")
                    }
                }
            }
        }
    }

    build(dumpName)

    return outputFile.readText()
}

val Project.propertiesExtension: ExtraPropertiesExtension
    get() = extensions.getByType(ExtraPropertiesExtension::class.java)

fun Project.enableUklibPublication(enable: Boolean = true) {
    propertiesExtension.set(
        PropertiesProvider.PropertyNames.KOTLIN_MPP_PUBLISH_UKLIB,
        enable.toString(),
    )
    if (enable) enableCrossCompilation()
}

fun Project.enableCrossCompilation(enable: Boolean = true) {
    propertiesExtension.set(
        PropertiesProvider.PropertyNames.KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION,
        enable.toString(),
    )
}