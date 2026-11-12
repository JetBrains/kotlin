/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.useXcodeMessageStyle
import org.jetbrains.kotlin.gradle.report.GradleBuildMetricsReporter
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionTypeProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.PlatformLibrariesGenerator
import org.jetbrains.kotlin.gradle.targets.native.internal.unzipTo
import org.jetbrains.kotlin.gradle.targets.native.konanPropertiesBuildService
import org.jetbrains.kotlin.gradle.targets.native.toolchain.NativeVersionValueSource
import org.jetbrains.kotlin.internal.compilerRunner.native.nativeCompilerClasspath
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class NativeCompilerDownloader(
    val project: Project,
) {

    companion object {
        val DEFAULT_KONAN_VERSION: String by lazy {
            loadPropertyFromResources("project.properties", "kotlin.native.version")
        }

        internal var NEED_TO_DOWNLOAD_FLAG: Boolean = true

        internal const val BASE_DOWNLOAD_URL = "https://download.jetbrains.com/kotlin/native/builds"
        internal const val KOTLIN_GROUP_ID = "org.jetbrains.kotlin"

        internal fun getCompilerDependencyNotation(project: Project): String {
            val group = KOTLIN_GROUP_ID
            val name = getDependencyName(project)
            val version = getCompilerVersion(project)
            val classifier = simpleOsName
            val ext = archiveExtension

            return "$group:$name:$version:$classifier@$ext"
        }

        internal fun getCompilerDirectory(
            project: Project,
            konanDataDirProperty: Provider<File>
        ): File {
            return DependencyDirectories
                .getLocalKonanDir(konanDataDirProperty.orNull?.absolutePath)
                .resolve(getDependencyNameWithOsAndVersion(project))
        }

        internal fun getDefaultCompilerDirectory(project: Project): File = DependencyDirectories
            .getLocalKonanDir(null)
            .resolve(getDependencyNameWithOsAndVersion(project))

        internal fun getOsSpecificCompilerDirectory(
            project: Project,
            konanDataDir: File,
        ): File = konanDataDir.resolve(getDependencyNameWithOsAndVersion(project))

        /**
         * Publishes the unpacked Kotlin/Native distribution from [source] to [destination] and reports whether
         * this caller is the one that installed it.
         *
         * @param move performs the actual move; overridable for tests
         * @return true when this caller published the distribution,
         *         false when another concurrent installation of [destination] won the race
         */
        internal fun publishExtractedDistribution(
            source: File,
            destination: File,
            move: () -> Unit = {
                // 'source' sits next to 'destination', so this is a rename: the whole distribution, marker
                // included, appears in one step. REPLACE_EXISTING only ever clears an empty leftover directory.
                Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            },
        ): Boolean {
            return try {
                move()
                true
            } catch (e: FileSystemException) {
                // several projects configured in parallel (for example, with Isolated Projects) may unpack
                // the same distribution at the same time, and 'Files.move' reports the loser in two shapes:
                //  - 'DirectoryNotEmptyException' when 'destination' was already occupied when the move
                //    checked it;
                //  - a plain 'FileSystemException' with the reason 'Directory not empty' when 'destination'
                //    appeared between that check and the rename, so the 'rename' itself failed with ENOTEMPTY,
                //    which the JDK does not translate into 'DirectoryNotEmptyException'.
                // Both shapes also report 'the move needed copying', which installs nothing, so only read them
                // as 'another build got here first' when there really is a distribution in place
                if (!destination.exists()) throw e
                // writing on top of that one would corrupt what the other build is reading (KT-86251)
                false
            }
        }

        internal fun getDependencyNameWithOsAndVersion(project: Project): String {
            return "${getDependencyName(project)}-$simpleOsName-${getCompilerVersion(project)}"
        }

        /**
         * Checks whether the distribution in [directory] was installed to the end: the marker file is written
         * only after the unpacked distribution is complete (KT-86251).
         */
        internal fun isDistributionProvisioned(directory: File): Boolean =
            directory.resolve(NativeVersionValueSource.MARKER_FILE).exists()


        private val simpleOsName = HostManager.platformName()

        private fun getCompilerVersion(project: Project): String {
            return project.nativeProperties.kotlinNativeVersion.get()
        }

        private fun getDependencyName(project: Project): String {
            val dependencySuffix =
                NativeDistributionTypeProvider(PropertiesProvider(project).nativeDistributionType).getDistributionType().suffix
            return if (dependencySuffix != null) {
                "kotlin-native-$dependencySuffix"
            } else {
                "kotlin-native"
            }
        }

        private val archiveExtension
            get() = if (useZip) {
                "zip"
            } else {
                "tar.gz"
            }

        private val useZip = HostManager.hostIsMingw

    }

    val compilerDirectory: File
        get() = getCompilerDirectory(project, project.nativeProperties.konanDataDir)

    private val logger: Logger
        get() = project.logger

    private val kotlinProperties get() = PropertiesProvider(project)

    private val downloadFromMaven = project.nativeProperties.downloadFromMaven

    private val dependencyNameWithOsAndVersion: String
        get() = getDependencyNameWithOsAndVersion(project)

    private val dependencyFileName: String
        get() = "$dependencyNameWithOsAndVersion.$archiveExtension"

    private fun setupRepo(repoUrl: String): ArtifactRepository {
        return project.repositories.ivy { repo ->
            repo.setUrl(repoUrl)
            repo.patternLayout {
                it.artifact("[artifact]-[revision].[ext]")
            }
            repo.metadataSources {
                it.artifact()
            }
        }
    }

    private fun removeRepo(repo: ArtifactRepository) {
        project.repositories.remove(repo)
    }

    private val repoUrl by lazy {
        val maturity = KotlinToolingVersion(getCompilerVersion(project)).maturity
        buildString {
            append("${kotlinProperties.nativeBaseDownloadUrl}/")
            append(if (maturity == KotlinToolingVersion.Maturity.DEV) "dev/" else "releases/")
            append("${getCompilerVersion(project)}/")
            append(simpleOsName)
        }
    }

    private fun downloadAndExtract() {
        val repo = if (!downloadFromMaven.get()) {
            setupRepo(repoUrl)
        } else null

        val compilerDependency = if (downloadFromMaven.get()) {
            project.dependencies.create(getCompilerDependencyNotation(project))
        } else {
            val name = "${getDependencyName(project)}-$simpleOsName"
            val version = getCompilerVersion(project)
            val ext = archiveExtension
            project.dependencies.create(":$name:$version@$ext")
        }

        val configuration = project.configurations.detachedResolvable(compilerDependency)
        logger.lifecycle("\nPlease wait while Kotlin/Native compiler ${getCompilerVersion(project)} is being installed.")

        if (!downloadFromMaven.get()) {
            val dependencyUrl = "$repoUrl/$dependencyFileName"
            val lengthSuffix = project.probeRemoteFileLength(dependencyUrl, probingTimeoutMs = 200)
                ?.let { " (${formatContentLength(it)})" }
                .orEmpty()
            logger.lifecycle("Download $dependencyUrl$lengthSuffix")
        }
        val archive = logger.lifecycleWithDuration("Download $dependencyFileName finished,") {
            configuration.files.single()
        }

        extractKotlinNativeFromArchive(archive)

        if (repo != null) removeRepo(repo)
    }

    private fun extractKotlinNativeFromArchive(archive: File) {
        logger.kotlinInfo("Using Kotlin/Native compiler archive: ${archive.absolutePath}")

        logger.lifecycle("Unpack Kotlin/Native compiler to $compilerDirectory")
        logger.lifecycleWithDuration("Unpack Kotlin/Native compiler to $compilerDirectory finished,") {
            // the extraction runs in a value source, like the toolchain installation (see NativeVersionValueSource):
            // the configuration cache does not observe value source file accesses, so checking the 'provisioned.ok'
            // marker and unpacking the distribution here does not invalidate the cache for the builds that follow
            // the one that installed the distribution
            project.providers.of(NativeCompilerDownloaderExtractor::class.java) {
                it.parameters.archivePath.set(archive.absolutePath)
                it.parameters.compilerDirectory.set(compilerDirectory.absolutePath)
                it.parameters.dependencyNameWithOsAndVersion.set(dependencyNameWithOsAndVersion)
            }.get()
        }
    }

    fun downloadIfNeeded() {
        checkClassPath() // This is workaround to avoid double execution configuration phase. See KT-61154 for more details
        if (NEED_TO_DOWNLOAD_FLAG) {
            downloadAndExtract()
        }
    }

    private fun checkClassPath() {
        project.providers.of(NativeCompilerDownloaderClassPathChecker::class.java) {
            it.parameters.classPath.setFrom(
                project.objects.nativeCompilerClasspath(
                    project.nativeProperties.actualNativeHomeDirectory
                )
            )
        }.get()
    }

    internal abstract class NativeCompilerDownloaderClassPathChecker :
        ValueSource<Boolean, NativeCompilerDownloaderClassPathChecker.Params> {

        interface Params : ValueSourceParameters {
            val classPath: ConfigurableFileCollection
        }

        override fun obtain(): Boolean {
            NEED_TO_DOWNLOAD_FLAG = parameters.classPath.files.none { it.exists() }
            return true
        }
    }

    /**
     * Unpacks the Kotlin/Native distribution from [Params.archivePath] into [Params.compilerDirectory], see
     * [NativeCompilerDownloader.downloadIfNeeded].
     *
     * Installed from the configuration phase through a value source, so that the configuration cache does not
     * observe the installation: the 'provisioned.ok' marker is created during the configuration that installs
     * the distribution, and a plain existence check of its absence would invalidate the cache for every
     * following build. The toolchain installs its distribution the same way, see NativeVersionValueSource.
     */
    internal abstract class NativeCompilerDownloaderExtractor :
        ValueSource<Boolean, NativeCompilerDownloaderExtractor.Params> {

        interface Params : ValueSourceParameters {
            val archivePath: Property<String>
            val compilerDirectory: Property<String>
            val dependencyNameWithOsAndVersion: Property<String>
        }

        private companion object {
            val logger: org.slf4j.Logger = LoggerFactory.getLogger(NativeCompilerDownloader::class.java)
        }

        override fun obtain(): Boolean {
            val archive = File(parameters.archivePath.get())
            val compilerDirectory = File(parameters.compilerDirectory.get())

            // with parallel project configuration (Isolated Projects), several projects unpack the same
            // distribution at the same time and race to publish the same compiler directory: the lock
            // serializes them, and the projects waiting for it observe the winner's finished installation
            // and skip the unpack entirely
            val lock = KotlinInterprocessDirectoryLock(compilerDirectory.parentFile) { message ->
                logger.info("Kotlin/Native compiler: $message")
            }
            lock.withLock {
                if (isDistributionProvisioned(compilerDirectory)) {
                    logger.debug("Kotlin/Native compiler is already installed in $compilerDirectory")
                    return@withLock
                }

                val kotlinNativeDir = compilerDirectory.parentFile.also { it.mkdirs() }
                val tmpDir = Files.createTempDirectory(kotlinNativeDir.toPath(), "compiler-").toFile()
                try {
                    logger.debug("Unpacking Kotlin/Native compiler to tmp directory $tmpDir")
                    unzipTo(archive, tmpDir)
                    val compilerTmp = tmpDir.resolve(parameters.dependencyNameWithOsAndVersion.get())
                    // the toolchain unpacks the archive over any distribution without this marker (KT-86251), so mark
                    // the tmp tree and let the move below publish the marker together with the distribution
                    NativeVersionValueSource.markAsProvisioned(compilerTmp)

                    if (publishExtractedDistribution(compilerTmp, compilerDirectory)) {
                        logger.debug("Moved Kotlin/Native compiler from $tmpDir to $compilerDirectory")
                    } else {
                        logger.debug("Kotlin/Native compiler is already installed in $compilerDirectory")
                    }
                } finally {
                    tmpDir.deleteRecursively()
                }
            }
            return true
        }
    }
}

/**
 * Sets up the Kotlin/Native compiler for the given project.
 *
 * @param konanTarget The target platform for the Kotlin/Native compiler.
 */
@Deprecated(
    message = "This is old k/n downloading method that is used on configuration phase",
    replaceWith = ReplaceWith(
        "KotlinNativeInstaller",
        "org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeInstaller"
    ),
    level = DeprecationLevel.WARNING
)
internal fun Project.setupNativeCompiler(konanTarget: KonanTarget) {
    val isKonanHomeOverridden = project.nativeProperties.userProvidedNativeHome.orNull != null
    if (!isKonanHomeOverridden) {
        val downloader = NativeCompilerDownloader(this)

        if (kotlinPropertiesProvider.nativeReinstall) {
            logger.info("Reinstall Kotlin/Native distribution")
            downloader.compilerDirectory.deleteRecursively()
        }

        downloader.downloadIfNeeded()
        logger.info("Kotlin/Native distribution: ${nativeProperties.actualNativeHomeDirectory.get().absolutePath}")
    } else {
        logger.info("User-provided Kotlin/Native distribution: ${nativeProperties.userProvidedNativeHome.orNull}")
    }

    val distributionType = NativeDistributionTypeProvider(PropertiesProvider(project).nativeDistributionType).getDistributionType()
    if (distributionType.mustGeneratePlatformLibs) {
        val nativeProperties = project.nativeProperties
        val konanPropertiesBuildService = project.konanPropertiesBuildService
        PlatformLibrariesGenerator(
            project.objects,
            konanTarget,
            project.kotlinPropertiesProvider.kotlinCompilerArgumentsLogLevel,
            konanPropertiesBuildService,
            project.objects.property(GradleBuildMetricsReporter()),
            ClassLoadersCachingBuildService.registerIfAbsent(project),
            PlatformLibrariesGenerator.registerRequiredServiceIfAbsent(project),
            project.useXcodeMessageStyle,
            project.objects.nativeCompilerClasspath(nativeProperties.actualNativeHomeDirectory),
            project.listProperty { nativeProperties.jvmArgs.get() },
            nativeProperties.actualNativeHomeDirectory,
            project.provider { nativeProperties.konanDataDir.orNull?.absolutePath },
            konanPropertiesBuildService.map { it.defaultCacheKindForTarget(konanTarget) },
        ).generatePlatformLibsIfNeeded()
    }
}
