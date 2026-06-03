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
import org.jetbrains.kotlin.gradle.targets.js.internal.calculateDirHash
import org.jetbrains.kotlin.gradle.targets.js.internal.toHex
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.mapOrNull
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
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

    @get:Input
    @get:Optional
    val downloadBaseUrlProvider: Provider<String> = env.mapOrNull(project.providers) {
        it.downloadBaseUrl
    }

    @get:Input
    val allowInsecureProtocol: Provider<Boolean> = env.map {
        it.allowInsecureProtocol
    }

    @get:OutputDirectory
    val destinationProvider: RegularFileProperty = project.objects.fileProperty()
        .fileProvider(env.map { it.dir })
        .also {
            it.disallowChanges()
        }

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

    @TaskAction
    fun exec() {
        if (!shouldDownload.get()) return

        logger.kotlinInfo("Using distribution from '$dist'")

        extractWithUpToDate(
            destinationProvider.getFile().toPath(),
            destinationHashFileProvider.getFile().toPath(),
            dist!!.toPath(),
            fileHasher,
            ::extract
        )
    }

    private fun <T> withUrlRepo(action: () -> T): T {
        val repo = downloadBaseUrlProvider.orNull?.let {
            project.repositories.ivy { repo ->
                repo.name = "Distributions at $it"
                repo.url = URI(it)

                repo.isAllowInsecureProtocol = allowInsecureProtocol.get()

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
        destination: Path,
        destinationHashFile: Path,
        dist: Path,
        fileHasher: FileHasher,
        extract: (File) -> Unit,
    ) {
        val upToDate = isUpToDate(
            destinationHashFile = destinationHashFile,
            destination = destination,
            dist = dist,
        )

        if (upToDate) return

        logger.info("[$path] Extracting distribution ${dist.fileName} to $destination")

        if (Files.isDirectory(destination)) {
            destination.toFile().deleteRecursively()
        }

        extract(dist.toFile())

        Files.newBufferedWriter(destinationHashFile).use {
            it.write(
                CACHE_VERSION +
                        " " +
                        fileHasher.calculateDirHash(destination)!! +
                        " " +
                        fileHasher.hash(dist.toFile()).toByteArray().toHex()
            )
        }
    }

    private fun isUpToDate(
        destinationHashFile: Path,
        destination: Path,
        dist: Path,
    ): Boolean {
        fun notUpToDate(reason: String): Boolean {
            logger.info("[$path] ${destination.fileName} Not up-to-date: $reason")
            return false
        }

        if (!Files.exists(destinationHashFile)) {
            return notUpToDate("no hash file $destinationHashFile")
        }

        val cacheData = Files.newBufferedReader(destinationHashFile).useLines { seq ->
            seq.firstOrNull().orEmpty().split(" ")
        }

        if (cacheData.size != 3) {
            val hashFileText = Files.newBufferedReader(destinationHashFile).use { it.readText() }
            return notUpToDate("invalid format $hashFileText")
        }

        if (cacheData[0] != CACHE_VERSION) {
            return notUpToDate("cache version mismatch ${cacheData[0]} != $CACHE_VERSION")
        }

        val expectedDestDirHash = cacheData[1]
        val currentDestDirHash = fileHasher.calculateDirHash(destination)
        if (expectedDestDirHash != currentDestDirHash) {
            return notUpToDate("destination hash mismatch expected:$expectedDestDirHash != current:$currentDestDirHash")
        }

        val expectedDistHash = cacheData[2]
        val currentDistHash = fileHasher.hash(dist.toFile()).toByteArray().toHex()
        if (expectedDistHash != currentDistHash) {
            return notUpToDate("distribution hash mismatch expected:$expectedDistHash != current:$currentDistHash")
        }

        logger.info("[$path] Distribution is up-to-date at $destination")
        return true
    }

    abstract fun extract(archive: File)

    companion object {
        const val CACHE_VERSION = "2"
    }
}
