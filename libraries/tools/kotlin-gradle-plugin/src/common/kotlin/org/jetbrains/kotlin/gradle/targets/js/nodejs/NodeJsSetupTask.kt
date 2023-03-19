package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.extractWithUpToDate
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

abstract class NodeJsSetupTask : DefaultTask() {
    @Transient
    private val settings = project.rootProject.kotlinNodeJsExtension
    private val env by lazy { settings.requireConfigured() }

    private val shouldDownload = settings.download

    @get:Inject
    abstract internal val archiveOperations: ArchiveOperations

    @get:Inject
    internal open val fileHasher: FileHasher
        get() = error("Should be injected")

    @get:Inject
    internal abstract val objects: ObjectFactory

    @get:Inject
    abstract internal val fs: FileSystemOperations

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
    @get:Optional
    val nodeJsDist: File? by lazy {
        if (shouldDownload) {
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
        } else null
    }

    init {
        onlyIf {
            shouldDownload
        }
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        if (!shouldDownload) return
        logger.kotlinInfo("Using node distribution from '$nodeJsDist'")

        extractWithUpToDate(
            destination,
            destinationHashFile,
            nodeJsDist!!,
            fileHasher
        ) { dist, destination ->
            var fixBrokenSymLinks = false

            fs.copy {
                it.from(
                    when {
                        dist.name.endsWith("zip") -> archiveOperations.zipTree(dist)
                        else -> {
                            fixBrokenSymLinks = true
                            archiveOperations.tarTree(dist)
                        }
                    }
                )
                it.into(destination)
            }

            fixBrokenSymlinks(destination, env.isWindows, fixBrokenSymLinks)

            if (!env.isWindows) {
                File(env.nodeExecutable).setExecutable(true)
            }
        }
    }

    private fun fixBrokenSymlinks(destinationDir: File, isWindows: Boolean, necessaryToFix: Boolean) {
        if (necessaryToFix) {
            val nodeBinDir = computeNodeBinDir(destinationDir, isWindows).toPath()
            fixBrokenSymlink("npm", nodeBinDir, destinationDir, isWindows)
            fixBrokenSymlink("npx", nodeBinDir, destinationDir, isWindows)
        }
    }

    private fun fixBrokenSymlink(
        name: String,
        nodeBinDirPath: Path,
        nodeDirProvider: File,
        isWindows: Boolean
    ) {
        val script = nodeBinDirPath.resolve(name)
        val scriptFile = computeNpmScriptFile(nodeDirProvider, name, isWindows)
        if (Files.deleteIfExists(script)) {
            Files.createSymbolicLink(script, nodeBinDirPath.relativize(Paths.get(scriptFile)))
        }
    }

    companion object {
        const val NAME: String = "kotlinNodeJsSetup"
    }
}
