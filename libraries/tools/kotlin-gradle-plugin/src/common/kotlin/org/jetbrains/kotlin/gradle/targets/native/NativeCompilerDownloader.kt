/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.compilerRunner.KotlinNativeToolRunner
import org.jetbrains.kotlin.compilerRunner.konanDataDir
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionType
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionTypeProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.PlatformLibrariesGenerator
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.nio.file.Files

class NativeCompilerDownloader(
    val project: Project,
    private val compilerVersion: String = project.konanVersion,
) {

    companion object {
        val DEFAULT_KONAN_VERSION: String by lazy {
            loadPropertyFromResources("project.properties", "kotlin.native.version")
        }

        internal var NEED_TO_DOWNLOAD_FLAG: Boolean = true

        internal const val BASE_DOWNLOAD_URL = "https://download.jetbrains.com/kotlin/native/builds"
        internal const val KOTLIN_GROUP_ID = "org.jetbrains.kotlin"
    }

    val compilerDirectory: File
        get() = DependencyDirectories
            .getLocalKonanDir(project.konanDataDir)
            .resolve(dependencyNameWithOsAndVersion)

    private val logger: Logger
        get() = project.logger

    private val kotlinProperties get() = PropertiesProvider(project)

    private val distributionType: NativeDistributionType
        get() = NativeDistributionTypeProvider(project).getDistributionType()

    private val simpleOsName: String
        get() = HostManager.platformName()

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
        val versionPattern = "(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-(\\p{Alpha}*\\p{Alnum}|[\\p{Alpha}-]*))?(?:-(\\d+))?".toRegex()
        val (_, _, _, buildType, _) = versionPattern.matchEntire(compilerVersion)?.destructured
            ?: error("Unable to parse version $compilerVersion")
        buildString {
            append("${kotlinProperties.nativeBaseDownloadUrl}/")
            append(if (buildType in listOf("RC", "RC2", "Beta") || buildType.isEmpty()) "releases/" else "dev/")
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
        checkClassPath() // This is workaround to avoid double execution configuration phase. See KT-61154 for more details
        if (NEED_TO_DOWNLOAD_FLAG) {
            downloadAndExtract()
        }
    }

    private fun checkClassPath() {
        project.providers.of(NativeCompilerDownloaderClassPathChecker::class.java) {
            it.parameters.classPath.setFrom(KotlinNativeToolRunner.Settings.of(project.konanHome, project.konanDataDir, project).classpath)
        }.usedAtConfigurationTime(project.configurationTimePropertiesAccessor).get()
    }

    internal abstract class NativeCompilerDownloaderClassPathChecker : ValueSource<Boolean, NativeCompilerDownloaderClassPathChecker.Params> {

        interface Params : ValueSourceParameters {
            val classPath: ConfigurableFileCollection
        }

        override fun obtain(): Boolean {
            NEED_TO_DOWNLOAD_FLAG = parameters.classPath.files.none { it.exists() }
            return true
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

    val distributionType = NativeDistributionTypeProvider(project).getDistributionType()
    if (distributionType.mustGeneratePlatformLibs) {
        PlatformLibrariesGenerator(project, konanTarget).generatePlatformLibsIfNeeded()
    }
}
