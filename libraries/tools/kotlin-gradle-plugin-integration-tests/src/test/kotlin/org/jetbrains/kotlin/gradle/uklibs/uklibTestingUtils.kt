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
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
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

fun Project.applyJvm(
    configure: KotlinJvmExtension.() -> Unit
) {
    plugins.apply("org.jetbrains.kotlin.jvm")
    (extensions.getByName("kotlin") as KotlinJvmExtension).configure()
}

data class PublisherConfiguration(
    val group: String = "foo",
    val version: String = "1.0",
    val repoPath: String = "repo",
) : Serializable

data class PublishedProject(
    val repository: File,
    val group: String,
    val name: String,
    val version: String,
) : Serializable {
    data class Component(
        private val path: File,
        private val artifactsPrefix: String,
    ) {
        val pom: File get() = path.resolve("${artifactsPrefix}.pom")
        val uklib: File get() = path.resolve("${artifactsPrefix}.uklib")
        val jar: File get() = path.resolve("${artifactsPrefix}.jar")
        val psmJar: File get() = path.resolve("${artifactsPrefix}-psm.jar")
        val gradleMetadata: File get() = path.resolve("${artifactsPrefix}.module")
    }

    val rootCoordinate: String = "$group:$name:$version"
    val rootComponent: Component
        get() = Component(
            repository.resolve(group).resolve(name).resolve(version),
            "${name}-${version}",
        )
    val jvmMultiplatformComponent: Component
        get() = Component(
            repository.resolve(group).resolve("${name}-jvm").resolve(version),
            "${name}-jvm-${version}",
        )
}

fun Project.setupMavenPublication(
    repositoryName: String,
    publisherConfiguration: PublisherConfiguration,
) {
    plugins.apply("maven-publish")

    group = publisherConfiguration.group
    version = publisherConfiguration.version

    val publishingExtension = extensions.getByType(PublishingExtension::class.java)
    publishingExtension.repositories.maven {
        it.name = repositoryName
        it.url = uri(
            layout.projectDirectory.dir(publisherConfiguration.repoPath)
        )
    }
}

fun TestProject.publishReturn(
    publisherConfiguration: PublisherConfiguration = PublisherConfiguration(
        group = "default_kotlin_${generateIdentifier()}"
    ),
    repositoryIdentifier: String,
): ReturnFromBuildScriptAfterExecution<PublishedProject> {
    buildScriptInjection {
        if (project.hasProperty(repositoryIdentifier)) {
            project.setupMavenPublication(repositoryIdentifier, publisherConfiguration)
        }
    }
    return buildScriptReturn {
        PublishedProject(
            project.layout.projectDirectory.dir(publisherConfiguration.repoPath).asFile,
            publisherConfiguration.group,
            project.name,
            publisherConfiguration.version,
        )
    }
}

fun TestProject.publish(
    vararg buildArguments: String = emptyArray(),
    publisherConfiguration: PublisherConfiguration = PublisherConfiguration(
        group = "default_kotlin_${generateIdentifier()}"
    ),
    deriveBuildOptions: TestProject.() -> BuildOptions = { buildOptions },
): PublishedProject {
    val repositoryIdentifier = "_KotlinPublication_${generateIdentifier()}_"
    return publishReturn(
        publisherConfiguration = publisherConfiguration,
        repositoryIdentifier = repositoryIdentifier,
    ).buildAndReturn(
        "publishAllPublicationsTo${repositoryIdentifier}Repository",
        "-P${repositoryIdentifier}",
        *buildArguments,
        deriveBuildOptions = deriveBuildOptions,
    )
}

fun TestProject.publishJava(
    publisherConfiguration: PublisherConfiguration = PublisherConfiguration(
        group = "default_java_${generateIdentifier()}"
    ),
    deriveBuildOptions: TestProject.() -> BuildOptions = { buildOptions },
): PublishedProject {
    val repositoryIdentifier = "_JavaPublication_${generateIdentifier()}_"
    buildScriptInjection {
        if (project.hasProperty(repositoryIdentifier)) {
            project.setupMavenPublication(repositoryIdentifier, publisherConfiguration)
            val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)

            publishingExtension.publications.create("java", MavenPublication::class.java) {
                it.from(project.components.getByName("java"))
            }
        }
    }
    return buildScriptReturn {
        PublishedProject(
            project.layout.projectDirectory.dir(publisherConfiguration.repoPath).asFile,
            publisherConfiguration.group,
            project.name,
            publisherConfiguration.version,
        )
    }.buildAndReturn(
        "publishAllPublicationsTo${repositoryIdentifier}Repository",
        "-P${repositoryIdentifier}",
        deriveBuildOptions = deriveBuildOptions,
    )
}

fun TestProject.addPublishedProjectToRepositories(
    publishedProject: PublishedProject,
    configuration: MavenArtifactRepository.() -> Unit = {},
) {
    transferDependencyResolutionRepositoriesIntoProjectRepositories()
    settingsBuildScriptInjection {
        settings.dependencyResolutionManagement.repositories.maven {
            it.url = publishedProject.repository.toURI()
            it.configuration()
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

fun TestProject.includeBuild(
    subproject: TestProject,
) {
    val includeBuildIdentifier = "included_${generateIdentifier()}"
    Files.createSymbolicLink(projectPath.resolve(includeBuildIdentifier), subproject.projectPath)
    settingsBuildScriptInjection {
        settings.includeBuild(includeBuildIdentifier)
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

                    FileOutputStream(outputFile).use {
                        val result = entryPoint.invoke(
                            null,
                            PrintStream(it),
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
    }

    build(dumpName)

    return outputFile.readText()
}

val Project.propertiesExtension: ExtraPropertiesExtension
    get() = extensions.getByType(ExtraPropertiesExtension::class.java)

internal fun Project.setUklibPublicationStrategy(strategy: KmpPublicationStrategy = KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication) {
    propertiesExtension.set(
        PropertiesProvider.PropertyNames.KOTLIN_KMP_PUBLICATION_STRATEGY,
        strategy.propertyName,
    )
    when (strategy) {
        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> Unit
        KmpPublicationStrategy.StandardKMPPublication -> Unit
    }
}

internal fun Project.setUklibResolutionStrategy(strategy: KmpResolutionStrategy = KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs) {
    propertiesExtension.set(
        PropertiesProvider.PropertyNames.KOTLIN_KMP_RESOLUTION_STRATEGY,
        strategy.propertyName,
    )
}

fun Project.computeTransformedLibraryChecksum(enable: Boolean = false) {
    propertiesExtension.set(
        PropertiesProvider.PropertyNames.KOTLIN_MPP_COMPUTE_TRANSFORMED_LIBRARY_CHECKSUM,
        enable.toString(),
    )
}
