package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.calculateDirHash
import org.jetbrains.kotlin.gradle.utils.ArchiveOperationsCompat
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import java.io.File
import java.net.URI
import javax.inject.Inject

abstract class NodeJsSetupTask : DefaultTask() {
    @Transient
    private val settings = NodeJsRootPlugin.apply(project.rootProject)
    private val env by lazy { settings.requireConfigured() }

    private val shouldDownload = settings.download

    private val archiveOperations = ArchiveOperationsCompat(project)

    @get:Inject
    internal open val fileHasher: FileHasher
        get() = error("Should be injected")

    @get:Inject
    internal open val fs: FileSystemOperations
        get() = error("Should be injected")

    val ivyDependency: String
        @Input get() = env.ivyDependency

    val downloadBaseUrl: String
        @Input get() = env.downloadBaseUrl

    val destination: File
        @OutputDirectory get() = env.nodeDir

    val destinationHashFile: File
        @OutputFile get() = destination.parentFile.resolve("${destination.name}.hash")

    @Transient
    @get:Internal
    internal lateinit var configuration: Provider<Configuration>

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
        val dist = configuration.get().files.single()
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

        var dirHash: String? = null
        val upToDate = destinationHashFile.let { file ->
            if (file.exists()) {
                file.useLines {
                    it.single() == (fileHasher.calculateDirHash(destination).also { dirHash = it })
                }
            } else false
        }

        val tmpDir = temporaryDir
        unpackNodeArchive(nodeJsDist, tmpDir)

        if (upToDate && fileHasher.calculateDirHash(tmpDir.resolve(destination.name))!! == dirHash) {
            tmpDir.deleteRecursively()
            return
        }

        if (destination.isDirectory) {
            destination.deleteRecursively()
        }

        fs.copy {
            it.from(tmpDir)
            it.into(destination.parentFile)
        }

        tmpDir.deleteRecursively()

        if (!env.isWindows) {
            File(env.nodeExecutable).setExecutable(true)
        }

        destinationHashFile.writeText(
            fileHasher.calculateDirHash(destination)!!
        )
    }

    private fun unpackNodeArchive(archive: File, destination: File) {
        logger.kotlinInfo("Unpacking $archive to $destination")

        fs.copy {
            it.from(fileTree(archive))
            it.into(destination)
        }
    }

    private fun fileTree(archive: File): FileTree =
        when {
            archive.name.endsWith("zip") -> archiveOperations.zipTree(archive)
            else -> archiveOperations.tarTree(archive)
        }

    companion object {
        const val NAME: String = "kotlinNodeJsSetup"
    }
}
