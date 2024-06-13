/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForBinariesCompilation
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionCommonizerLock
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionTypeProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.PlatformLibrariesGenerator
import org.jetbrains.kotlin.gradle.targets.native.konanPropertiesBuildService
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.konan.properties.KonanPropertiesLoader
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.loadConfigurables
import org.jetbrains.kotlin.konan.util.ArchiveExtractor
import org.jetbrains.kotlin.konan.util.ArchiveType
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.GZIPInputStream
import javax.inject.Inject

private const val MARKER_FILE = "provisioned.ok"

internal interface UsesKotlinNativeBundleBuildService : Task {
    @get:Internal
    val kotlinNativeBundleBuildService: Property<KotlinNativeBundleBuildService>
}

/**
 * This service provides functionality to prepare a Kotlin/Native bundle.
 */
internal abstract class KotlinNativeBundleBuildService : BuildService<KotlinNativeBundleBuildService.Parameters> {

    internal interface Parameters : BuildServiceParameters {
        val kotlinNativeVersion: Property<String>
    }

    @get:Inject
    abstract val fso: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    private var canBeReinstalled: Boolean = true // we can reinstall a k/n bundle once during the build

    companion object {
        fun registerIfAbsent(project: Project): Provider<KotlinNativeBundleBuildService> =
            project.gradle.sharedServices.registerIfAbsent(
                "kotlinNativeBundleBuildService",
                KotlinNativeBundleBuildService::class.java
            ) {
                it.parameters.kotlinNativeVersion
                    .value(project.nativeProperties.kotlinNativeVersion)
                    .disallowChanges()
            }.also { serviceProvider ->
                SingleActionPerProject.run(project, UsesKotlinNativeBundleBuildService::class.java.name) {
                    project.tasks.withType<UsesKotlinNativeBundleBuildService>().configureEach { task ->
                        task.kotlinNativeBundleBuildService.value(serviceProvider).disallowChanges()
                        task.usesService(serviceProvider)
                    }
                }
            }
    }

    /**
     * This function downloads and installs a Kotlin Native bundle if needed
     * and then prepares its platform libraries if needed.
     *
     * @param project The Gradle project object.
     * @param kotlinNativeBundleConfiguration Gradle configuration for Kotlin Native Bundle
     * @param kotlinNativeVersion The version of Kotlin/Native to install
     * @param bundleDir The directory to store the Kotlin/Native bundle.
     * @param reinstallFlag A flag indicating whether to reinstall the bundle.
     * @param konanTargets The set of KonanTarget objects representing the targets for the Kotlin/Native bundle.
     * @param overriddenKonanHome Overridden konan home if present.
     * @return kotlin native version if toolchain was used, path to konan home if konan home was used
     */
    internal fun prepareKotlinNativeBundle(
        project: Project,
        kotlinNativeBundleConfiguration: ConfigurableFileCollection,
        kotlinNativeVersion: String,
        bundleDir: File,
        reinstallFlag: Boolean,
        konanTargets: Set<KonanTarget>,
        overriddenKonanHome: String?,
    ) {
        if (overriddenKonanHome != null) {
            project.logger.info("A user-provided Kotlin/Native distribution configured: ${overriddenKonanHome}. Disabling Kotlin Native Toolchain auto-provisioning.")
        } else {
            processToolchain(bundleDir, project, reinstallFlag, kotlinNativeVersion, kotlinNativeBundleConfiguration)
        }

        project.setupKotlinNativePlatformLibraries(konanTargets)
    }

    private fun processToolchain(
        bundleDir: File,
        project: Project,
        reinstallFlag: Boolean,
        kotlinNativeVersion: String,
        kotlinNativeBundleConfiguration: ConfigurableFileCollection,
    ) {
        val lock =
            NativeDistributionCommonizerLock(bundleDir) { message -> project.logger.info("Kotlin Native Bundle: $message") }

        lock.withLock {
            val needToReinstall =
                KotlinToolingVersion(parameters.kotlinNativeVersion.get()).maturity == KotlinToolingVersion.Maturity.SNAPSHOT
            if (needToReinstall) {
                project.logger.debug("Snapshot version could be changed, to be sure that up-to-date version is used, Kotlin/Native should be reinstalled")
            }

            removeBundleIfNeeded(reinstallFlag || needToReinstall, bundleDir)

            if (!bundleDir.resolve(MARKER_FILE).exists()) {
                val gradleCachesKotlinNativeDir =
                    resolveKotlinNativeConfiguration(kotlinNativeVersion, kotlinNativeBundleConfiguration)

                project.logger.info("Moving Kotlin/Native bundle from tmp directory $gradleCachesKotlinNativeDir to ${bundleDir.absolutePath}")
                fso.copy {
                    it.from(gradleCachesKotlinNativeDir)
                    it.into(bundleDir)
                }
                createSuccessfulInstallationFile(bundleDir)
                project.logger.info("Moved Kotlin/Native bundle from $gradleCachesKotlinNativeDir to ${bundleDir.absolutePath}")
            }
        }
    }

    /**
     * Downloads native dependencies for Kotlin Native based on the provided configuration.
     * @return A set of required dependencies that were downloaded.
     */
    internal fun downloadNativeDependencies(
        bundleDir: File,
        konanDataDir: String?,
        konanTargets: Set<KonanTarget>,
        logger: Logger,
    ): Set<String> {
        val requiredDependencies = mutableSetOf<String>()
        val distribution = Distribution(bundleDir.absolutePath, konanDataDir = konanDataDir)
        konanTargets.forEach { konanTarget ->
            if (konanTarget.enabledOnCurrentHostForBinariesCompilation()) {
                val konanPropertiesLoader = loadConfigurables(
                    konanTarget,
                    distribution.properties,
                    distribution.dependenciesDir,
                    progressCallback = { url, currentBytes, totalBytes ->
                        logger.info("Downloading dependency for Kotlin Native: $url (${currentBytes}/${totalBytes}). ")
                    }
                ) as KonanPropertiesLoader

                requiredDependencies.addAll(konanPropertiesLoader.dependencies)
                konanPropertiesLoader.downloadDependencies(DependencyExtractor())
            }
        }
        return requiredDependencies
    }

    private fun removeBundleIfNeeded(
        reinstallFlag: Boolean,
        bundleDir: File,
    ) {
        if (reinstallFlag && canBeReinstalled) {
            bundleDir.deleteRecursively()
            canBeReinstalled = false // we don't need to reinstall k/n if it was reinstalled once during the same build
        }
    }

    private fun resolveKotlinNativeConfiguration(
        kotlinNativeVersion: String,
        kotlinNativeCompilerConfiguration: ConfigurableFileCollection,
    ): File {
        val resolutionErrorMessage = "Kotlin Native dependency has not been properly resolved. " +
                "Please, make sure that you've declared the repository, which contains $kotlinNativeVersion."

        val gradleCachesKotlinNativeDir = kotlinNativeCompilerConfiguration
            .singleOrNull()
            ?.resolve(kotlinNativeVersion)
            ?: error(resolutionErrorMessage)

        if (!gradleCachesKotlinNativeDir.exists()) {
            error(
                "Kotlin Native bundle dependency was used. " +
                        "Please provide the corresponding version in 'kotlin.native.version' property instead of any other ways."
            )
        }
        return gradleCachesKotlinNativeDir
    }

    private fun Project.setupKotlinNativePlatformLibraries(konanTargets: Set<KonanTarget>) {
        val distributionType = NativeDistributionTypeProvider(this).getDistributionType()
        if (distributionType.mustGeneratePlatformLibs) {
            konanTargets.forEach { konanTarget ->
                PlatformLibrariesGenerator(
                    project,
                    konanTarget,
                    project.konanHome,
                    project.kotlinPropertiesProvider,
                    project.konanPropertiesBuildService,
                ).generatePlatformLibsIfNeeded()
            }
        }
    }

    private inner class DependencyExtractor : ArchiveExtractor {

        override fun extract(archive: File, targetDirectory: File, archiveType: ArchiveType) {
            when (archiveType) {
                ArchiveType.ZIP -> archiveOperations.zipTree(archive)
                ArchiveType.TAR_GZ -> unzipTarGz(archive, targetDirectory)
                else -> error("Unsupported format for unzipping $archive")
            }
        }

        private fun unzipTarGz(archive: File, targetDir: File) {
            GZIPInputStream(BufferedInputStream(archive.inputStream())).use { gzipInputStream ->
                val hardLinks = HashMap<Path, Path>()

                TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
                    generateSequence {
                        tarInputStream.nextEntry
                    }.forEach { entry: TarArchiveEntry ->
                        val outputFile = File("$targetDir/${entry.name}")
                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            if (entry.isSymbolicLink) {
                                Files.createSymbolicLink(outputFile.toPath(), Paths.get(entry.linkName))
                            } else if (entry.isLink) {
                                hardLinks.put(outputFile.toPath(), targetDir.resolve(entry.linkName).toPath())
                            } else {
                                outputFile.outputStream().use {
                                    tarInputStream.copyTo(it)
                                }
                                Files.setPosixFilePermissions(outputFile.toPath(), getPosixFilePermissions(entry.mode))
                            }
                        }
                    }
                }
                hardLinks.forEach {
                    Files.createLink(it.key, it.value)
                }
            }
        }

        private fun getPosixFilePermissions(mode: Int): Set<PosixFilePermission> {
            val permissions: MutableSet<PosixFilePermission> = mutableSetOf()

            // adding owner permissions
            permissions.addPermission(mode, 0b100_000_000, PosixFilePermission.OWNER_READ)
            permissions.addPermission(mode, 0b010_000_000, PosixFilePermission.OWNER_WRITE)
            permissions.addPermission(mode, 0b001_000_000, PosixFilePermission.OWNER_EXECUTE)

            // adding group permissions
            permissions.addPermission(mode, 0b000_100_000, PosixFilePermission.GROUP_READ)
            permissions.addPermission(mode, 0b000_010_000, PosixFilePermission.GROUP_WRITE)
            permissions.addPermission(mode, 0b000_001_000, PosixFilePermission.GROUP_EXECUTE)

            // adding other permissions
            permissions.addPermission(mode, 0b000_000_100, PosixFilePermission.OTHERS_READ)
            permissions.addPermission(mode, 0b000_000_010, PosixFilePermission.OTHERS_WRITE)
            permissions.addPermission(mode, 0b000_000_001, PosixFilePermission.OTHERS_EXECUTE)

            return permissions
        }

        private fun MutableSet<PosixFilePermission>.addPermission(mode: Int, permissionBitMask: Int, permission: PosixFilePermission) {
            if ((mode and permissionBitMask) > 0) {
                add(permission)
            }
        }
    }

    private fun createSuccessfulInstallationFile(bundleDir: File) {
        bundleDir.resolve(MARKER_FILE).createNewFile()
    }
}
