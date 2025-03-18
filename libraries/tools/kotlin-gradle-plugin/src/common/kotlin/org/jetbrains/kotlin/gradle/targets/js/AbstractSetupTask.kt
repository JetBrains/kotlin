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
import java.io.RandomAccessFile
import java.net.URI
import java.nio.channels.FileChannel
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

    @Deprecated(
        "Use ivyDependencyProvider instead. It uses Gradle Provider API. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DeprecatedCallableAddReplaceWith")
    val ivyDependency: String
        @Internal get() = ivyDependencyProvider.get()

    @get:Input
    @get:Optional
    val downloadBaseUrlProvider: Provider<String> = env.mapOrNull(project.providers) {
        it.downloadBaseUrl
    }

    @get:Input
    val allowInsecureProtocol: Provider<Boolean> = env.map {
        it.allowInsecureProtocol
    }

    @Deprecated(
        "Use downloadBaseUrlProvider instead. It uses Gradle Provider API. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DeprecatedCallableAddReplaceWith")
    val downloadBaseUrl: String?
        @Internal
        get() = downloadBaseUrlProvider.orNull

    @get:OutputDirectory
    val destinationProvider: RegularFileProperty = project.objects.fileProperty()
        .fileProvider(env.map { it.dir })
        .also {
            it.disallowChanges()
        }

    @Deprecated(
        "Use destinationProvider instead. It uses Gradle Provider API. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DeprecatedCallableAddReplaceWith")
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

    @Deprecated(
        "Use destinationHashFileProvider instead. It uses Gradle Provider API. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("DeprecatedCallableAddReplaceWith")
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
        super.onlyIf {
            shouldDownload.get()
        }
    }

    @TaskAction
    fun exec() {
        if (!shouldDownload.get()) return

        logger.kotlinInfo("Using distribution from '$dist'")

        extractWithUpToDate(
            destinationProvider.getFile(),
            dist!!,
        )
    }

    private fun <T> withUrlRepo(action: () -> T): T = runWithHashFileLock { _ ->
        val repo = downloadBaseUrlProvider.orNull?.let { downloadBaseUrl ->
            project.repositories.ivy { repo ->
                repo.name = "Distributions at $downloadBaseUrl"
                repo.url = URI(downloadBaseUrl)

                repo.isAllowInsecureProtocol = allowInsecureProtocol.get()

                repo.patternLayout {
                    it.artifact(artifactPattern)
                }
                repo.metadataSources { it.artifact() }
                repo.content { it.includeModule(artifactModule, artifactName) }
            }
        }

        val result = action()

        if (repo != null) {
            project.repositories.remove(repo)
        }

        return result
    }

    private fun extractWithUpToDate(
        destination: File,
        dist: File,
    ): Unit = runWithHashFileLock { hashFile ->
        val currentHash = computeCurrentHash()

        val storedHash =
            if (hashFile.length() > 0) {
                hashFile.readLine().trim()
            } else {
                "<hashFile missing>"
            }

        if (currentHash == storedHash) {
            logger.info("[$path] Skipping download. dist:$dist and destination:$destination are up-to-date. ($currentHash == $storedHash).")
            return
        }

        if (destination.isDirectory) {
            destination.deleteRecursively()
        }

        extract(dist)

        logger.info("[$path] Extracted distribution to $dist")

        val updatedHash = computeCurrentHash()
            ?: error("failed to compute hash. destination:$destination, dist:$dist.")
        hashFile.writeUTF(updatedHash)
    }

    private fun computeCurrentHash(): String? {
        val destination = destinationProvider.getFile()
        val dist = dist ?: return null

        return buildString {
            val actualDestinationHash = fileHasher.calculateDirHash(destination) ?: return null

            val actualDistHash =
                if (dist.exists()) {
                    fileHasher.hash(dist).toByteArray().toHex()
                } else {
                    return null
                }

            append(CACHE_VERSION)
            append(" ")
            append(actualDestinationHash)
            append(" ")
            append(actualDistHash)
        }.trim()
    }

    abstract fun extract(archive: File)

    /**
     * Prevent concurrent execution across threads & JVMs.
     *
     * ### The problem
     *
     * Concurrent downloading and unpacking tools can cause issues when they are run in parallel.
     * This can result in the downloaded files having a size of zero,
     * or the permissions of the unpacked files not being properly configured.
     *
     * Concurrent execution happens more often when running KGP integration tests locally,
     * since the tests are run in parallel with multiple Gradle versions.
     *
     * ### The solution
     *
     * This function executes [action] under two locks to prevent concurrent execution.
     *
     * - Within the same JVM process: use [synchronized].
     * - Across JVM processes: use [java.nio.channels.FileLock], using [destinationHashFileProvider].
     */
    @OptIn(ExperimentalContracts::class)
    private inline fun <T> runWithHashFileLock(action: (hashFile: RandomAccessFile) -> T): T {
        contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }

        synchronized(Companion) {
            val lockFile = destinationHashFileProvider.get().asFile.apply {
                parentFile.mkdirs()
                createNewFile()
            }

            RandomAccessFile(lockFile, "rw").use { file ->
                val channel: FileChannel = file.channel
                channel.lock().use { _ ->
                    return action(file)
                }
            }
        }
    }

    companion object {
        const val CACHE_VERSION = "2"
    }
}
