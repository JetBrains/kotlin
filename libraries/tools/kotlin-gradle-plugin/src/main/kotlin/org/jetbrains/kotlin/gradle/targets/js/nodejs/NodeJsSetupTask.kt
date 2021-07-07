package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.utils.ArchiveOperationsCompat
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import java.io.File
import java.net.URI
import javax.inject.Inject

@CacheableTask
abstract class NodeJsSetupTask : DefaultTask() {
    @Transient
    private val settings = NodeJsRootPlugin.apply(project.rootProject)
    private val env by lazy { settings.requireConfigured() }

    private val shouldDownload = settings.download

    private val archiveOperations = ArchiveOperationsCompat(project)

    @get:Inject
    internal open val fs: FileSystemOperations
        get() = error("Should be injected")

    val ivyDependency: String
        @Input get() = env.ivyDependency

    val downloadBaseUrl: String
        @Input get() = env.downloadBaseUrl

    val destination: File
        @OutputDirectory get() = env.nodeDir

    @Transient
    @get:Internal
    internal lateinit var configuration: Provider<Configuration>

    private val _nodeJsDist by lazy {
        configuration.get().files.single()
    }

    @get:Classpath
    val nodeJsDist: File by lazy {
        val repo = project.repositories.ivy { repo ->
            repo.name = "Node Distributions at ${downloadBaseUrl}"
            repo.url = URI(downloadBaseUrl)

            repo.patternLayout {
                it.artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            repo.metadataSources { it.artifact() }
            repo.content { it.includeModule("org.nodejs", "node") }
        }
        val startDownloadTime = System.currentTimeMillis()
        val dist = _nodeJsDist
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

    @Suppress("unused")
    @TaskAction
    fun exec() {
        logger.kotlinInfo("Using node distribution from '$nodeJsDist'")

        unpackNodeArchive(nodeJsDist, destination.parentFile) // parent because archive contains name already

        if (!env.isWindows) {
            File(env.nodeExecutable).setExecutable(true)
        }
    }

    private fun unpackNodeArchive(archive: File, destination: File) {
        logger.kotlinInfo("Unpacking $archive to $destination")

        when {
            archive.name.endsWith("zip") -> fs.copy {
                it.from(archiveOperations.zipTree(archive))
                it.into(destination)
            }
            else -> {
                fs.copy {
                    it.from(archiveOperations.tarTree(archive))
                    it.into(destination)
                }
            }
        }
    }

    companion object {
        const val NAME: String = "kotlinNodeJsSetup"
    }
}
