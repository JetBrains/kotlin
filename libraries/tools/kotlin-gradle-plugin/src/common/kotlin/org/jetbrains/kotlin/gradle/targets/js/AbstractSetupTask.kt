package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.internal.hash.FileHasher
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import java.io.File
import java.net.URI
import javax.inject.Inject

@DisableCachingByDefault
abstract class AbstractSetupTask<Env : AbstractEnv, Settings : AbstractSettings<Env>> : DefaultTask() {
    @get:Internal
    protected abstract val settings: Settings

    @get:Internal
    protected abstract val artifactPattern: String

    @get:Internal
    protected abstract val artifactModule: String

    @get:Internal
    protected abstract val artifactName: String

    @get:Internal
    protected val env: Env by lazy { settings.requireConfigured() }

    private val shouldDownload by lazy {
        env.download
    }

    @get:Inject
    internal abstract val archiveOperations: ArchiveOperations

    @get:Inject
    internal abstract val fileHasher: FileHasher

    @get:Inject
    internal abstract val objects: ObjectFactory

    @get:Inject
    internal abstract val fs: FileSystemOperations

    val ivyDependency: String
        @Input get() = env.ivyDependency

    val downloadBaseUrl: String?
        @Input
        @Optional
        get() = env.downloadBaseUrl

    val destination: File
        @OutputDirectory get() = env.dir

    val destinationHashFile: File
        @OutputFile get() = destination.parentFile.resolve("${destination.name}.hash")

    @Transient
    @get:Internal
    internal var configuration: Provider<Configuration>? = null

    @get:Classpath
    @get:Optional
    val dist: File? by lazy {
        if (!shouldDownload) return@lazy null

        withUrlRepo {
            val startDownloadTime = System.currentTimeMillis()
            configuration!!.get().files.single().also {
                val downloadDuration = System.currentTimeMillis() - startDownloadTime
                if (downloadDuration > 0) {
                    KotlinBuildStatsService.getInstance()
                        ?.report(NumericalMetrics.ARTIFACTS_DOWNLOAD_SPEED, it.length() * 1000 / downloadDuration)
                }
            }
        }
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

        logger.kotlinInfo("Using distribution from '$dist'")

        extractWithUpToDate(
            destination,
            destinationHashFile,
            dist!!,
            fileHasher,
            ::extract
        )
    }

    private fun <T> withUrlRepo(action: () -> T): T {
        val repo = downloadBaseUrl?.let {
            project.repositories.ivy { repo ->
                repo.name = "Distributions at ${it}"
                repo.url = URI(it)

                repo.patternLayout {
                    it.artifact(artifactPattern)
                }
                repo.metadataSources { it.artifact() }
                repo.content { it.includeModule(artifactModule, artifactName) }
            }
        }

        return action().also {
            repo?.let { project.repositories.remove(it) }
        }
    }

    private fun extractWithUpToDate(
        destination: File,
        destinationHashFile: File,
        dist: File,
        fileHasher: FileHasher,
        extract: (File, File) -> Unit,
    ) {
        var distHash: String? = null
        val upToDate = destinationHashFile.let { file ->
            if (file.exists()) {
                file.useLines { seq ->
                    val list = seq.first().split(" ")
                    list.size == 2 &&
                            list[0] == fileHasher.calculateDirHash(destination) &&
                            list[1] == fileHasher.hash(dist).toByteArray().toHex().also { distHash = it }
                }
            } else false
        }

        if (upToDate) {
            return
        }

        if (destination.isDirectory) {
            destination.deleteRecursively()
        }

        extract(dist, destination.parentFile)

        destinationHashFile.writeText(
            fileHasher.calculateDirHash(destination)!! +
                    " " +
                    (distHash ?: fileHasher.hash(dist).toByteArray().toHex())
        )
    }

    abstract fun extract(archive: File, destination: File)
}
