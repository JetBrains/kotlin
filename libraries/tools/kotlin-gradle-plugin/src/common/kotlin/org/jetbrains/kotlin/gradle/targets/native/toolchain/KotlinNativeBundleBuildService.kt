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
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.konanTargets
import org.jetbrains.kotlin.compilerRunner.getKonanCacheKind
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForBinariesCompilation
import org.jetbrains.kotlin.gradle.report.GradleBuildMetricsReporter
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionTypeProvider
import org.jetbrains.kotlin.gradle.targets.native.internal.PlatformLibrariesGenerator
import org.jetbrains.kotlin.gradle.targets.native.internal.getNativeDistributionDependencies
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.konan.properties.KonanPropertiesLoader
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.loadConfigurables
import org.jetbrains.kotlin.konan.util.ArchiveExtractor
import org.jetbrains.kotlin.konan.util.ArchiveType
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.GZIPInputStream
import javax.inject.Inject

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
        val classLoadersCachingService: Property<ClassLoadersCachingBuildService>
        val konanPropertiesBuildService: Property<KonanPropertiesBuildService>
        val platformLibrariesGeneratorService: Property<PlatformLibrariesGenerator.GeneratedPlatformLibrariesService>
    }

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    companion object {
        private val log = Logging.getLogger(KotlinNativeBundleBuildService::class.java)

        fun registerIfAbsent(project: Project): Provider<KotlinNativeBundleBuildService> {
            val classLoadersCachingService = ClassLoadersCachingBuildService.registerIfAbsent(project)
            val konanPropertiesBuildService = KonanPropertiesBuildService.registerIfAbsent(project)
            val platformLibrariesService = PlatformLibrariesGenerator.registerRequiredServiceIfAbsent(project)
            return project.gradle.sharedServices.registerIfAbsent(
                "kotlinNativeBundleBuildService",
                KotlinNativeBundleBuildService::class.java
            ) {
                it.parameters.kotlinNativeVersion
                    .value(project.nativeProperties.kotlinNativeVersion)
                    .disallowChanges()
                it.parameters.classLoadersCachingService.value(classLoadersCachingService).disallowChanges()
                it.parameters.konanPropertiesBuildService.value(konanPropertiesBuildService).disallowChanges()
                it.parameters.platformLibrariesGeneratorService.value(platformLibrariesService).disallowChanges()
            }.also { serviceProvider ->
                SingleActionPerProject.run(project, UsesKotlinNativeBundleBuildService::class.java.name) {
                    project.tasks.withType<UsesKotlinNativeBundleBuildService>().configureEach { task ->
                        task.kotlinNativeBundleBuildService.value(serviceProvider).disallowChanges()
                        task.usesService(serviceProvider)
                    }
                }
            }
        }

        internal fun getNativeDistributionDependencies(
            project: Project,
            commonizerTarget: CommonizerTarget,
            kotlinNativeBundleBuildService: Provider<KotlinNativeBundleBuildService>,
        ): FileCollection {
            val kotlinNativeProvider =
                KotlinNativeFromToolchainProvider(project, commonizerTarget.konanTargets, kotlinNativeBundleBuildService)
            return project.getNativeDistributionDependencies(
                kotlinNativeProvider.konanDistributionProvider,
                commonizerTarget
            )
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
    ): Set<String> {
        val requiredDependencies = mutableSetOf<String>()
        val distribution = Distribution(bundleDir.absolutePath, konanDataDir = konanDataDir)
        konanTargets.forEach { konanTarget ->
            if (konanTarget.enabledOnCurrentHostForBinariesCompilation) {
                val konanPropertiesLoader = loadConfigurables(
                    konanTarget,
                    distribution.properties,
                    distribution.dependenciesDir,
                    progressCallback = { url, currentBytes, totalBytes ->
                        log.info("Downloading dependency for Kotlin Native: $url (${currentBytes}/${totalBytes}). ")
                    }
                ) as KonanPropertiesLoader

                requiredDependencies.addAll(konanPropertiesLoader.dependencies)
                konanPropertiesLoader.downloadDependencies(DependencyExtractor())
            }
        }
        return requiredDependencies
    }

    fun setupKotlinNativePlatformLibraries(
        objectFactory: ObjectFactory,
        konanTargetsWithNativeCacheKind: Map<KonanTarget, Provider<NativeCacheKind>>,
        nativeDistributionType: String?,
        kotlinCompilerArgumentsLogLevel: Provider<KotlinCompilerArgumentsLogLevel>,
        useXcodeMessageStyle: Provider<Boolean>,
        classpath: FileCollection,
        jvmArgs: ListProperty<String>,
        actualNativeHomeDirectory: Provider<File>,
        konanDataDir: Provider<String?>,
    ) {
        val distributionType = NativeDistributionTypeProvider(nativeDistributionType).getDistributionType()
        if (distributionType.mustGeneratePlatformLibs) {
            konanTargetsWithNativeCacheKind.forEach { (konanTarget, nativeCacheKind) ->
                PlatformLibrariesGenerator(
                    objectFactory,
                    konanTarget,
                    kotlinCompilerArgumentsLogLevel,
                    parameters.konanPropertiesBuildService,
                    objectFactory.property(GradleBuildMetricsReporter()),
                    parameters.classLoadersCachingService,
                    parameters.platformLibrariesGeneratorService,
                    useXcodeMessageStyle,
                    classpath,
                    jvmArgs,
                    actualNativeHomeDirectory,
                    konanDataDir,
                    nativeCacheKind,
                ).generatePlatformLibsIfNeeded()
            }
        }
    }

    internal fun getNativeCacheKind(project: Project, konanTargets: KonanTarget) =
        project.nativeProperties.getKonanCacheKind(konanTargets, parameters.konanPropertiesBuildService)

    private inner class DependencyExtractor : ArchiveExtractor {

        override fun extract(archive: File, targetDirectory: File, archiveType: ArchiveType) {
            when (archiveType) {
                ArchiveType.ZIP -> archive.toPath().unzipTo(targetDirectory.toPath())
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

}
