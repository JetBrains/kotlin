package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.internal.hash.FileHasher
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.statistics.UrlRepoConfigurationMetrics
import org.jetbrains.kotlin.gradle.plugin.statistics.UsesBuildFusService
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.mapOrNull
import java.io.File
import java.net.URI
import javax.inject.Inject

@DisableCachingByDefault
abstract class AbstractSetupTask<Env : AbstractEnv, Spec : EnvSpec<Env>>(
    spec: Spec,
) : DefaultTask(), UsesBuildFusService {

    @get:Internal
    protected abstract val artifactPattern: String

    @get:Internal
    protected abstract val artifactModule: String

    @get:Internal
    protected abstract val artifactName: String

    @get:Internal
    protected val env: Provider<Env> = spec.env

    private val shouldDownload: Provider<Boolean> = env.map { it.download }

    @get:Inject
    internal abstract val archiveOperations: ArchiveOperations

    @get:Inject
    internal abstract val fileHasher: FileHasher

    @get:Inject
    internal abstract val objects: ObjectFactory

    @get:Inject
    internal abstract val fs: FileSystemOperations

    @get:Input
    internal val ivyDependencyProvider: Provider<String> = env.map { it.ivyDependency }

    @Deprecated("Use ivyDependencyProvider instead. It uses Gradle Provider API.")
    val ivyDependency: String
        @Internal get() = ivyDependencyProvider.get()

    @get:Input
    @get:Optional
    val downloadBaseUrlProvider: Provider<String> = env.mapOrNull(project.providers) {
        it.downloadBaseUrl
    }

    @Deprecated("Use downloadBaseUrlProvider instead. It uses Gradle Provider API.")
    val downloadBaseUrl: String?
        @Internal
        get() = downloadBaseUrlProvider.orNull

    @get:OutputDirectory
    val destinationProvider: RegularFileProperty = project.objects.fileProperty()
        .fileProvider(env.map { it.dir })
        .also {
            it.disallowChanges()
        }

    @Deprecated("Use destinationProvider instead. It uses Gradle Provider API.")
    val destination: File
        @Internal get() = destinationProvider.getFile()

    @get:OutputFile
    val destinationHashFileProvider: RegularFileProperty = project.objects.fileProperty()
        .fileProvider(
            destinationProvider.locationOnly.map {
                val file = it.asFile
                file.parentFile.resolve("${file.name}.hash")
            }
        ).also {
            it.disallowChanges()
        }

    @Deprecated("Use destinationHashFileProvider instead. It uses Gradle Provider API.")
    val destinationHashFile: File
        @Internal get() = destinationHashFileProvider.getFile()

    @Transient
    @get:Internal
    internal var configuration: Provider<Configuration>? = null

    @get:Classpath
    @get:Optional
    val dist: File? by lazy {
        if (!shouldDownload.get()) return@lazy null

        withUrlRepo {
            val startDownloadTime = System.currentTimeMillis()
            configuration!!.get().files.single().also {
                val downloadDuration = System.currentTimeMillis() - startDownloadTime
                if (downloadDuration > 0) {
                    buildFusService.orNull?.reportFusMetrics { metricsConsumer ->
                        UrlRepoConfigurationMetrics.collectMetrics(it.length(), downloadDuration, metricsConsumer)
                    }
                }
            }
        }
    }

    init {
        onlyIf {
            shouldDownload.get()
        }
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        if (!shouldDownload.get()) return

        logger.kotlinInfo("Using distribution from '$dist'")

        extractWithUpToDate(
            destinationProvider.getFile(),
            destinationHashFileProvider.getFile(),
            dist!!,
            fileHasher,
            ::extract
        )
    }

    private fun <T> withUrlRepo(action: () -> T): T {
        val repo = downloadBaseUrlProvider.orNull?.let {
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
        extract: (File) -> Unit,
    ) {
        var distHash: String? = null
        val upToDate = destinationHashFile.let { file ->
            if (file.exists()) {
                file.useLines { seq ->
                    val list = seq.first().split(" ")
                    list.size == 3 &&
                            list[0] == CACHE_VERSION &&
                            list[1] == fileHasher.calculateDirHash(destination) &&
                            list[2] == fileHasher.hash(dist).toByteArray().toHex().also { distHash = it }
                }
            } else false
        }

        if (upToDate) {
            return
        }

        if (destination.isDirectory) {
            destination.deleteRecursively()
        }

        extract(dist)

        destinationHashFile.writeText(
            CACHE_VERSION +
                    " " +
                    fileHasher.calculateDirHash(destination)!! +
                    " " +
                    (distHash ?: fileHasher.hash(dist).toByteArray().toHex())
        )
    }

    abstract fun extract(archive: File)

    companion object {
        const val CACHE_VERSION = "2"
    }
}
