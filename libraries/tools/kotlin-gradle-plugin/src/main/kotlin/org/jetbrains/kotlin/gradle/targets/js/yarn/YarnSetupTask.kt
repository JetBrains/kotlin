/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.utils.ArchiveOperationsCompat
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import java.io.File
import java.net.URI
import javax.inject.Inject

@CacheableTask
open class YarnSetupTask : DefaultTask() {
    @Transient
    private val settings = project.yarn
    private val env by lazy { settings.requireConfigured() }

    private val shouldDownload = settings.download

    private val archiveOperations = ArchiveOperationsCompat(project)

    @get:Inject
    internal open val fs: FileSystemOperations
        get() = error("Should be injected")

    @Suppress("MemberVisibilityCanBePrivate")
    val downloadUrl
        @Input get() = env.downloadUrl

    @Suppress("MemberVisibilityCanBePrivate")
    val destination: File
        @OutputDirectory get() = env.home

    init {
        group = NodeJsRootPlugin.TASKS_GROUP_NAME
        description = "Download and install a local yarn version"
    }

    val ivyDependency: String
        @Input get() = env.ivyDependency

    @Transient
    @get:Internal
    internal lateinit var configuration: Provider<Configuration>

    private val _yarnDist by lazy {
        configuration.get().files.single()
    }

    @get:Classpath
    val yarnDist: File by lazy {
        val repo = project.repositories.ivy { repo ->
            repo.name = "Yarn Distributions at ${downloadUrl}"
            repo.url = URI(downloadUrl)
            repo.patternLayout {
                it.artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            repo.metadataSources { it.artifact() }
            repo.content { it.includeModule("com.yarnpkg", "yarn") }
        }
        val startDownloadTime = System.currentTimeMillis()
        val dist = _yarnDist
        val downloadDuration = System.currentTimeMillis() - startDownloadTime
        if (downloadDuration > 0) {
            KotlinBuildStatsService.getInstance()
                ?.report(NumericalMetrics.ARTIFACTS_DOWNLOAD_SPEED, dist.length() * 1000 / downloadDuration)
        }
        project.repositories.remove(repo)
        dist
    }

    init {
        onlyIf {
            shouldDownload
        }
    }

    @TaskAction
    fun setup() {
        logger.kotlinInfo("Using yarn distribution from '$yarnDist'")

        extract(yarnDist, destination.parentFile) // parent because archive contains name already
    }

    private fun extract(archive: File, destination: File) {
        val dirInTar = archive.name.removeSuffix(".tar.gz")
        fs.copy {
            it.from(archiveOperations.tarTree(archive))
            it.into(destination)
            it.includeEmptyDirs = false
            it.eachFile { fileCopy ->
                fileCopy.path = fileCopy.path.removePrefix(dirInTar)
            }
        }
    }

    companion object {
        const val NAME: String = "kotlinYarnSetup"
    }
}
