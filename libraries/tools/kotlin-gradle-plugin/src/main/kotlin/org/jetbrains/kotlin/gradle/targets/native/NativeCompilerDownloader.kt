/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCompilerRunner
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.dsl.NativeDistributionType
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File

class NativeCompilerDownloader(
    val project: Project,
    private val compilerVersion: CompilerVersion = project.konanVersion
) {

    companion object {
        val DEFAULT_KONAN_VERSION: CompilerVersion by lazy {
            CompilerVersion.fromString(loadPropertyFromResources("project.properties", "kotlin.native.version"))
        }

        private const val BASE_DOWNLOAD_URL = "https://download.jetbrains.com/kotlin/native/builds"
    }

    val compilerDirectory: File
        get() = DependencyDirectories.localKonanDir.resolve(dependencyNameWithVersion)

    private val logger: Logger
        get() = project.logger

    private val distributionType: NativeDistributionType
        get() = PropertiesProvider(project).nativeDistributionType?.takeIf {
            it.isAvailableFor(HostManager.host, compilerVersion)
        } ?: NativeDistributionType.REGULAR

    private val simpleOsName: String
        get() = HostManager.simpleOsName()

    private val dependencyName: String
        get() {
            val dependencySuffix = distributionType.suffix
            return if (dependencySuffix != null) {
                "kotlin-native-$dependencySuffix-$simpleOsName"
            } else {
                "kotlin-native-$simpleOsName"
            }
        }

    private val dependencyNameWithVersion: String
        get() = "$dependencyName-$compilerVersion"

    private val dependencyFileName: String
        get() = "$dependencyNameWithVersion.$archiveExtension"

    private val useZip
        get() = HostManager.hostIsMingw

    private val archiveExtension
        get() = if (useZip) {
            "zip"
        } else {
            "tar.gz"
        }

    private fun archiveFileTree(archive: File): FileTree =
        if (useZip) {
            project.zipTree(archive)
        } else {
            project.tarTree(archive)
        }

    private fun setupRepo(repoUrl: String): ArtifactRepository {
        return project.repositories.ivy { repo ->
            repo.setUrl(repoUrl)
            repo.patternLayoutCompatible {
                artifact("[artifact]-[revision].[ext]")
            }
            repo.metadataSources {
                it.artifact()
            }
        }
    }

    private fun removeRepo(repo: ArtifactRepository) {
        project.repositories.remove(repo)
    }

    private fun downloadAndExtract() {
        val repoUrl = buildString {
            append("$BASE_DOWNLOAD_URL/")
            append(if (compilerVersion.meta == MetaVersion.DEV) "dev/" else "releases/")
            append("$compilerVersion/")
            append(simpleOsName)
        }
        val dependencyUrl = "$repoUrl/$dependencyFileName"

        val repo = setupRepo(repoUrl)

        val compilerDependency = project.dependencies.create(
            mapOf(
                "name" to dependencyName,
                "version" to compilerVersion.toString(),
                "ext" to archiveExtension
            )
        )

        val configuration = project.configurations.detachedConfiguration(compilerDependency)
        logger.lifecycle("\nPlease wait while Kotlin/Native compiler $compilerVersion is being installed.")

        val suffix = project.probeRemoteFileLength(dependencyUrl, probingTimeoutMs = 200)?.let { " (${formatContentLength(it)})" }.orEmpty()
        logger.lifecycle("Download $dependencyUrl$suffix")
        val archive = logger.lifecycleWithDuration("Download $dependencyUrl finished,") {
            configuration.files.single()
        }

        logger.kotlinInfo("Using Kotlin/Native compiler archive: ${archive.absolutePath}")

        logger.lifecycle("Unpack Kotlin/Native compiler to $compilerDirectory")
        logger.lifecycleWithDuration("Unpack Kotlin/Native compiler to $compilerDirectory finished,") {
            project.copy {
                it.from(archiveFileTree(archive))
                it.into(DependencyDirectories.localKonanDir)
            }
        }

        removeRepo(repo)
    }

    fun downloadIfNeeded() {
        if (KotlinNativeCompilerRunner(project).classpath.isEmpty()) {
            downloadAndExtract()
        }
    }
}