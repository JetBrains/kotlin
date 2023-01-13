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
import org.jetbrains.kotlin.compilerRunner.KotlinNativeToolRunner
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionType
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionTypeProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.PlatformLibrariesGenerator
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.CompilerVersionImpl
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.isAtLeast
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.nio.file.Files

class NativeCompilerDownloader(
    val project: Project,
    private val compilerVersion: CompilerVersion = project.konanVersion
) {

    companion object {
        val DEFAULT_KONAN_VERSION: CompilerVersion by lazy {
            CompilerVersion.fromString(loadPropertyFromResources("project.properties", "kotlin.native.version"))
        }

        internal const val BASE_DOWNLOAD_URL = "https://download.jetbrains.com/kotlin/native/builds"
        internal const val KOTLIN_GROUP_ID = "org.jetbrains.kotlin"
    }

    val compilerDirectory: File
        get() = DependencyDirectories.localKonanDir.resolve(dependencyNameWithOsAndVersion)

    private val logger: Logger
        get() = project.logger

    private val kotlinProperties get() = PropertiesProvider(project)

    private val distributionType: NativeDistributionType
        get() = NativeDistributionTypeProvider(project).getDistributionType(compilerVersion)

    private val simpleOsName: String
        get() {
            return if (compilerVersion.isAtLeast(CompilerVersionImpl(major = 1, minor = 5, maintenance = 30, build = 1466))) {
                HostManager.platformName()
            } else {
                HostManager.simpleOsName()
            }
        }

    private val dependencyName: String
        get() {
            val dependencySuffix = distributionType.suffix
            return if (dependencySuffix != null) {
                "kotlin-native-$dependencySuffix"
            } else {
                "kotlin-native"
            }
        }

    private val dependencyNameWithOsAndVersion: String
        get() = "$dependencyName-$simpleOsName-$compilerVersion"

    private val dependencyFileName: String
        get() = "$dependencyNameWithOsAndVersion.$archiveExtension"

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
            repo.patternLayout {
                it.artifact("[artifact]-[revision].[ext]")
            }
            repo.metadataSources {
                it.artifact()
            }
        }
    }

    private fun removeRepo(repo: ArtifactRepository) {
        project.repositories.remove(repo)
    }

    private val repoUrl by lazy {
        buildString {
            append("${kotlinProperties.nativeBaseDownloadUrl}/")
            append(if (compilerVersion.meta == MetaVersion.DEV) "dev/" else "releases/")
            append("$compilerVersion/")
            append(simpleOsName)
        }
    }

    private fun downloadAndExtract() {
        val repo = if (!kotlinProperties.nativeDownloadFromMaven) {
            setupRepo(repoUrl)
        } else null

        val compilerDependency = if (kotlinProperties.nativeDownloadFromMaven) {
            project.dependencies.create(
                mapOf(
                    "group" to KOTLIN_GROUP_ID,
                    "name" to dependencyName,
                    "version" to compilerVersion.toString(),
                    "classifier" to simpleOsName,
                    "ext" to archiveExtension
                )
            )
        } else {
            project.dependencies.create(
                mapOf(
                    "name" to "$dependencyName-$simpleOsName",
                    "version" to compilerVersion.toString(),
                    "ext" to archiveExtension
                )
            )
        }

        val configuration = project.configurations.detachedConfiguration(compilerDependency)
            .markResolvable()
        logger.lifecycle("\nPlease wait while Kotlin/Native compiler $compilerVersion is being installed.")

        if (!kotlinProperties.nativeDownloadFromMaven) {
            val dependencyUrl = "$repoUrl/$dependencyFileName"
            val lengthSuffix = project.probeRemoteFileLength(dependencyUrl, probingTimeoutMs = 200)
                ?.let { " (${formatContentLength(it)})" }
                .orEmpty()
            logger.lifecycle("Download $dependencyUrl$lengthSuffix")
        }
        val archive = logger.lifecycleWithDuration("Download $dependencyFileName finished,") {
            configuration.files.single()
        }

        logger.kotlinInfo("Using Kotlin/Native compiler archive: ${archive.absolutePath}")

        logger.lifecycle("Unpack Kotlin/Native compiler to $compilerDirectory")
        logger.lifecycleWithDuration("Unpack Kotlin/Native compiler to $compilerDirectory finished,") {
            val kotlinNativeDir = compilerDirectory.parentFile.also { it.mkdirs() }
            val tmpDir = Files.createTempDirectory(kotlinNativeDir.toPath(), "compiler-").toFile()
            try {
                logger.debug("Unpacking Kotlin/Native compiler to tmp directory $tmpDir")
                project.copy {
                    it.from(archiveFileTree(archive))
                    it.into(tmpDir)
                }
                val compilerTmp = tmpDir.resolve(dependencyNameWithOsAndVersion)
                if (!compilerTmp.renameTo(compilerDirectory)) {
                    project.copy {
                        it.from(compilerTmp)
                        it.into(compilerDirectory)
                    }
                }
                logger.debug("Moved Kotlin/Native compiler from $tmpDir to $compilerDirectory")
            } finally {
                tmpDir.deleteRecursively()
            }
        }

        if (repo != null) removeRepo(repo)
    }

    fun downloadIfNeeded() {

        val classpath = KotlinNativeToolRunner.Settings.fromProject(project).classpath
        if (classpath.isEmpty() || classpath.any { !it.exists() }) {
            downloadAndExtract()
        }
    }
}

internal fun Project.setupNativeCompiler(konanTarget: KonanTarget) {
    val isKonanHomeOverridden = kotlinPropertiesProvider.nativeHome != null
    if (!isKonanHomeOverridden) {
        val downloader = NativeCompilerDownloader(this)

        if (kotlinPropertiesProvider.nativeReinstall) {
            logger.info("Reinstall Kotlin/Native distribution")
            downloader.compilerDirectory.deleteRecursively()
        }

        downloader.downloadIfNeeded()
        logger.info("Kotlin/Native distribution: $konanHome")
    } else {
        logger.info("User-provided Kotlin/Native distribution: $konanHome")
    }

    val distributionType = NativeDistributionTypeProvider(project).getDistributionType(konanVersion)
    if (distributionType.mustGeneratePlatformLibs) {
        PlatformLibrariesGenerator(project, konanTarget).generatePlatformLibsIfNeeded()
    }
}