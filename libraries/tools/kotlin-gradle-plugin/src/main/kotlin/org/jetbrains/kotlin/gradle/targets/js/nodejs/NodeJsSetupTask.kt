package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import java.io.File
import java.net.URI

@CacheableTask
open class NodeJsSetupTask : DefaultTask() {
    private val settings = NodeJsRootPlugin.apply(project.rootProject)
    private val env by lazy { settings.requireConfigured() }
    private val fs = services.get(FileSystemOperations::class.java)
    private val archives: Any? = try {
        services.get(ArchiveOperations::class.java)
    } catch (e: NoClassDefFoundError) {
        // Gradle version < 6.6
        null
    }

    val ivyDependency: String
        @Input get() = env.ivyDependency

    val destination: File
        @OutputDirectory get() = env.nodeDir

    @Transient
    @get:Internal
    internal lateinit var configuration: Provider<Configuration>

    private val _nodeJsDist by lazy {
        configuration.get().files.single()
    }

    @Suppress("unused") // as it called by Gradle before task execution and used to resolve artifact
    @get:Classpath
    val nodeJsDist: File
        get() {
            @Suppress("UnstableApiUsage", "DEPRECATION")
            val repo = project.repositories.ivy { repo ->
                repo.name = "Node Distributions at ${settings.nodeDownloadBaseUrl}"
                repo.url = URI(settings.nodeDownloadBaseUrl)

                repo.patternLayout {
                    it.artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                    it.ivy("v[revision]/ivy.xml")
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
            return dist
        }

    init {
        @Suppress("LeakingThis")
        onlyIf {
            settings.download && !File(env.nodeExecutable).isFile
        }
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        logger.kotlinInfo("Using node distribution from '$_nodeJsDist'")

        unpackNodeArchive(_nodeJsDist, destination.parentFile) // parent because archive contains name already

        if (!env.isWindows) {
            File(env.nodeExecutable).setExecutable(true)
        }
    }

    private fun unpackNodeArchive(archive: File, destination: File) {
        logger.kotlinInfo("Unpacking $archive to $destination")

        when {
            archive.name.endsWith("zip") -> fs.copy {
                val from = if (archives != null) {
                    (archives as ArchiveOperations).zipTree(archive)
                } else {
                    project.zipTree(archive)
                }
                it.from(from)
                it.into(destination)
            }
            else -> {
                fs.copy {
                    val from = if (archives != null) {
                        (archives as ArchiveOperations).tarTree(archive)
                    } else {
                        project.tarTree(archive)
                    }
                    it.from(from)
                    it.into(destination)
                }
            }
        }
    }

    companion object {
        const val NAME: String = "kotlinNodeJsSetup"
    }
}
