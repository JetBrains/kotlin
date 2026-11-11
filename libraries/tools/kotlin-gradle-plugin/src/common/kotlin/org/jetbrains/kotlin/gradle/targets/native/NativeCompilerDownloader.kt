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
import org.gradle.api.logging.Logging
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
         * Installs the Kotlin/Native distribution into [compilerDirectory], unless a complete one is already there.
         *
         * Projects configured in parallel (for example, with Isolated Projects) all ask for the same distribution
         * at the same time. Only one of them unpacks it; the rest wait for the lock and then find it installed.
         *
         * @param unpackTo unpacks the distribution archive into the given directory. The archive is expected to
         * contain a single directory named like [compilerDirectory].
         */
        internal fun installDistribution(
            compilerDirectory: File,
            logger: Logger,
            unpackTo: (directory: File) -> Unit,
        ) {
            val konanDir = compilerDirectory.parentFile
            // One lock per distribution, kept next to it. It can't live inside like the toolchain's lock does:
            // taking the lock creates its directory, and the rename below needs the target to be absent or empty.
            val lock = KotlinInterprocessDirectoryLock(konanDir.resolve(".${compilerDirectory.name}.lock")) { message ->
                logger.info("Kotlin/Native compiler: $message")
            }
            lock.withLock {
                if (NativeVersionValueSource.isProvisioned(compilerDirectory)) {
                    logger.debug("Kotlin/Native compiler is already installed in $compilerDirectory")
                    return@withLock
                }

                val tmpDir = Files.createTempDirectory(konanDir.toPath(), "compiler-").toFile()
                try {
                    logger.debug("Unpacking Kotlin/Native compiler to tmp directory $tmpDir")
                    unpackTo(tmpDir)
                    val compilerTmp = tmpDir.resolve(compilerDirectory.name)
                    // The toolchain unpacks over any distribution that has no marker (KT-86251). Marking the tmp
                    // tree means the marker shows up together with the distribution, never after it.
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
        }

        /**
         * Moves the unpacked Kotlin/Native distribution from [source] to [destination].
         *
         * @return true if this call installed the distribution, false if a complete one was already in place
         */
        internal fun publishExtractedDistribution(source: File, destination: File): Boolean {
            return try {
                // 'source' sits next to 'destination', so this is a rename and the whole distribution appears
                // in one step. REPLACE_EXISTING only ever removes an empty leftover directory.
                Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                true
            } catch (e: FileSystemException) {
                // An installer that doesn't take our lock can still get here first. The JDK reports that in
                // several ways: DirectoryNotEmptyException if 'destination' was there before the move, a plain
                // FileSystemException if it appeared halfway through, AccessDeniedException on Windows (KTI-3755).
                // The same exceptions also mean "nothing got installed", and the directory may exist with only
                // the toolchain's lock file in it, so the marker is the only proof that a distribution is there.
                if (!NativeVersionValueSource.isProvisioned(destination)) throw e
                // Leave it alone: another build may be reading it right now (KT-86251).
                false
            }
        }

        internal fun getDependencyNameWithOsAndVersion(project: Project): String {
            return "${getDependencyName(project)}-$simpleOsName-${getCompilerVersion(project)}"
        }


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
            // Runs as a value source because it looks at the 'provisioned.ok' marker. Checked directly, the
            // marker would become a configuration cache input and invalidate the cache on the build right after
            // the installation. NativeVersionValueSource does the same.
            project.providers.of(NativeCompilerDownloaderExtractor::class.java) {
                it.parameters.archivePath.set(archive.absolutePath)
                it.parameters.compilerDirectory.set(compilerDirectory.absolutePath)
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
     * Unpacks the Kotlin/Native distribution archive into the compiler directory, see [installDistribution].
     */
    internal abstract class NativeCompilerDownloaderExtractor :
        ValueSource<Boolean, NativeCompilerDownloaderExtractor.Params> {

        interface Params : ValueSourceParameters {
            val archivePath: Property<String>
            val compilerDirectory: Property<String>
        }

        override fun obtain(): Boolean {
            val archive = File(parameters.archivePath.get())
            installDistribution(File(parameters.compilerDirectory.get()), logger) { unzipTo(archive, it) }
            return true
        }

        private companion object {
            val logger: Logger = Logging.getLogger(NativeCompilerDownloader::class.java)
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
