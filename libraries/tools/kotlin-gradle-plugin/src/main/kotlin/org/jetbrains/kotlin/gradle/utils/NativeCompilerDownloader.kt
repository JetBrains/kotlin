/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.compilerRunner.KonanCompilerRunner
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.KonanVersionImpl
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File

class NativeCompilerDownloader(
    val project: Project,
    val compilerVersion: KonanVersion = project.konanVersion
) {

    internal companion object {
        val DEFAULT_KONAN_VERSION = KonanVersionImpl(MetaVersion.RELEASE, 0, 9, 3)
        const val BASE_DOWNLOAD_URL = "https://download.jetbrains.com/kotlin/native/builds"
    }

    private val logger: Logger
        get() = project.logger

    private val simpleOsName: String
        get() = HostManager.simpleOsName()

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

    private fun setupRepo(url: String): ArtifactRepository {
        return project.repositories.ivy { repo ->
            repo.setUrl(url)
            repo.layout("pattern") {
                val layout = it as IvyPatternRepositoryLayout
                layout.artifact("[artifact]-[revision].[ext]")
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
        val versionString = compilerVersion.toString()

        val url = buildString {
            append("$BASE_DOWNLOAD_URL/")
            append(if (compilerVersion.meta == MetaVersion.DEV) "dev/" else "releases/")
            append("$versionString/")
            append(simpleOsName)
        }

        val repo = setupRepo(url)

        val compilerDependency = project.dependencies.create(
            mapOf(
                "name" to "kotlin-native-$simpleOsName",
                "version" to versionString,
                "ext" to archiveExtension
            )
        )

        val configuration = project.configurations.detachedConfiguration(compilerDependency)
        val archive = configuration.files.single()

        logger.info("Use Kotlin/Native compiler archive: ${archive.absolutePath}")
        logger.lifecycle("Unpack Kotlin/Native compiler (version $versionString)...")
        project.copy {
            it.from(archiveFileTree(archive))
            it.into(DependencyDirectories.localKonanDir)
        }

        removeRepo(repo)
    }

    val compilerDirectory: File
        get() = DependencyDirectories.localKonanDir.resolve("kotlin-native-$simpleOsName-$compilerVersion")

    fun downloadIfNeeded() {
        if (KonanCompilerRunner(project).classpath.isEmpty) {
            downloadAndExtract()
        }
    }
}