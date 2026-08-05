/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
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
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionTypeProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.PlatformLibrariesGenerator
import org.jetbrains.kotlin.gradle.targets.native.konanPropertiesBuildService
import org.jetbrains.kotlin.internal.compilerRunner.native.nativeCompilerClasspath
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

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
            konanDataDirProperty: Provider<File?>
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

        // KT-86251 diagnostics: opt-in flag (`-Pkotlin.native.diagnostics.readOnlyDistribution=true`) to make the
        // freshly extracted distribution read-only, so any process overwriting shipped files fails with a stack trace.
        internal const val READ_ONLY_DISTRIBUTION_DIAGNOSTICS_PROPERTY = "kotlin.native.diagnostics.readOnlyDistribution"

        // The distribution's runtime cache directory; there is no shared constant for it in `konan.library`.
        private const val KONAN_DISTRIBUTION_CACHE_DIR = "cache"

        private val WRITE_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.OTHERS_WRITE,
        )

        /**
         * Diagnostic aid for KT-86251. Makes the freshly extracted Kotlin/Native distribution immutable by
         * stripping the write bit from every file and directory (read/execute bits are preserved, so binaries
         * stay runnable and directories stay listable). Any later attempt to overwrite or delete a *shipped*
         * distribution file then fails loudly with a permission error whose stack trace names the writer.
         *
         * Two build-time scratch locations inside the distribution are exempted, otherwise the build cannot run
         * at all:
         * - `klib/cache`: only the directory node, so the compiler can add new runtime cache flavors
         *   (`klib/cache/<flavor>/...`). Its *shipped* children (the `-system` per-file caches) stay read-only —
         *   they are the files reported as missing or corrupted in KT-86251.
         * - `klib/commonized`: the whole subtree, because nothing there is shipped. It is where
         *   `NativeDistributionCommonizerTask` writes, including the `.lock` file it needs before it can even
         *   answer its own up-to-date check.
         *
         * POSIX-only; a no-op on filesystems without POSIX permissions (e.g. Windows).
         */
        internal fun markDistributionReadOnly(distribution: File, logger: Logger) {
            if (!distribution.toPath().fileSystem.supportedFileAttributeViews().contains("posix")) {
                logger.info("KT-86251 diagnostics: read-only distribution requested, but the filesystem is not POSIX; skipping")
                return
            }
            // Create the scratch dirs up front (while still writable) so they can be exempted below.
            val klibDir = distribution.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            val cacheDir = klibDir.resolve(KONAN_DISTRIBUTION_CACHE_DIR).also { it.mkdirs() }
            val commonizedDir = klibDir.resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR).also { it.mkdirs() }

            distribution.walkTopDown()
                .filterNot { it.startsWith(commonizedDir) }
                .forEach { setWritable(it.toPath(), writable = false, logger = logger) }
            // Re-grant write on the cache directory node only (not its shipped children), so the compiler can
            // still create new cache flavors while the shipped tree stays immutable.
            setWritable(cacheDir.toPath(), writable = true, logger = logger)
            logger.lifecycle(
                "KT-86251 diagnostics: marked Kotlin/Native distribution read-only at $distribution " +
                        "($KONAN_DISTRIBUTION_KLIB_DIR/$KONAN_DISTRIBUTION_CACHE_DIR and " +
                        "$KONAN_DISTRIBUTION_KLIB_DIR/$KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR kept writable)"
            )
        }

        /**
         * Restores the write bit recursively. Needed before deleting a distribution previously marked
         * read-only by [markDistributionReadOnly] (e.g. on reinstall), otherwise the delete would fail.
         */
        internal fun restoreDistributionWritable(distribution: File) {
            if (!distribution.exists()) return
            if (!distribution.toPath().fileSystem.supportedFileAttributeViews().contains("posix")) return
            distribution.walkTopDown().forEach { setWritable(it.toPath(), writable = true, logger = null) }
        }

        private fun setWritable(path: Path, writable: Boolean, logger: Logger?) {
            try {
                val permissions = Files.getPosixFilePermissions(path).toMutableSet()
                if (writable) {
                    permissions.add(PosixFilePermission.OWNER_WRITE)
                } else {
                    permissions.removeAll(WRITE_PERMISSIONS)
                }
                Files.setPosixFilePermissions(path, permissions)
            } catch (e: IOException) {
                logger?.info("KT-86251 diagnostics: could not update permissions of $path: ${e.message}")
            }
        }

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

    private fun archiveFileTree(archive: File): FileTree =
        if (useZip) {
            project.zipTree(archive)
        } else {
            project.tarTree(archive)
        }

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
            val kotlinNativeDir = compilerDirectory.parentFile.also { it.mkdirs() }
            val tmpDir = Files.createTempDirectory(kotlinNativeDir.toPath(), "compiler-").toFile()
            try {
                logger.debug("Unpacking Kotlin/Native compiler to tmp directory $tmpDir")
                project.copy {
                    it.from(archiveFileTree(archive))
                    it.into(tmpDir)
                }
                val compilerTmp = tmpDir.resolve(dependencyNameWithOsAndVersion)
                // Don't copy over an already-present installation: it races with concurrent readers and, once the
                // distribution is marked read-only for diagnostics (KT-86251), would fail with EACCES from here.
                if (!compilerTmp.renameTo(compilerDirectory) && !compilerDirectory.exists()) {
                    project.copy {
                        it.from(compilerTmp)
                        it.into(compilerDirectory)
                    }
                }
                logger.debug("Moved Kotlin/Native compiler from $tmpDir to $compilerDirectory")
                markDistributionReadOnlyForDiagnostics(compilerDirectory)
            } finally {
                tmpDir.deleteRecursively()
            }
        }
    }

    private val readOnlyDistributionDiagnosticsEnabled: Boolean
        get() = project.providers
            .gradleProperty(READ_ONLY_DISTRIBUTION_DIAGNOSTICS_PROPERTY)
            .map { it.toBoolean() }
            .getOrElse(false)

    // KT-86251 diagnostics: opt-in aid to detect who overwrites the shipped Kotlin/Native distribution.
    private fun markDistributionReadOnlyForDiagnostics(distribution: File) {
        if (!readOnlyDistributionDiagnosticsEnabled) return
        markDistributionReadOnly(distribution, logger)
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
            // KT-86251 diagnostics: the distribution may have been marked read-only; restore write so it can be deleted.
            NativeCompilerDownloader.restoreDistributionWritable(downloader.compilerDirectory)
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
