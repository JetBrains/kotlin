/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.apache.commons.io.file.FilesUncheck.copy
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeDistributionCommonizerLock
import java.io.File
import javax.inject.Inject

internal abstract class NativeVersionValueSource :
    ValueSource<String, NativeVersionValueSource.Params> {

    interface Params : ValueSourceParameters {
        val bundleDirectory: Property<String>
        val reinstallBundle: Property<Boolean>
        val overriddenKonanHome: Property<String>
        val dependencyNameWithOsAndVersion: Property<String>
        val toolchainEnabled: Property<Boolean>
        val kotlinNativeCompilerConfiguration: Property<ConfigurableFileCollection>

        //        val logger: Property<Logger>
        val kotlinNativeVersion: Property<String>
        val fileSystemOperations: Property<FileSystemOperations>
    }

    //
    @get:Inject
    abstract val kotlinNativeBundleBuildService: KotlinNativeBundleBuildService

    override fun obtain(): String {
        val kotlinNativeVersion =
            if (parameters.overriddenKonanHome.isPresent)
                parameters.overriddenKonanHome.get()
            else
                parameters.dependencyNameWithOsAndVersion.get()
        if (parameters.toolchainEnabled.get()) {
            prepareKotlinNativeBundle(
////                    parameters.logger.get(),
                parameters.kotlinNativeCompilerConfiguration.get(),
                kotlinNativeVersion,
                File(parameters.bundleDirectory.get()),
                parameters.reinstallBundle.get(),
                parameters.overriddenKonanHome.orNull,
            )
        }
        return kotlinNativeVersion
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
    private fun prepareKotlinNativeBundle(
//        projectLogger: Logger,
        kotlinNativeBundleConfiguration: ConfigurableFileCollection,
        kotlinNativeVersion: String,
        bundleDir: File,
        reinstallFlag: Boolean,
        overriddenKonanHome: String?,
    ) {
        if (overriddenKonanHome != null) {
//            projectLogger.info("A user-provided Kotlin/Native distribution configured: ${overriddenKonanHome}. Disabling Kotlin Native Toolchain auto-provisioning.")
            println("A user-provided Kotlin/Native distribution configured: ${overriddenKonanHome}. Disabling Kotlin Native Toolchain auto-provisioning.")
        } else {
            processToolchain(bundleDir, reinstallFlag, kotlinNativeVersion, kotlinNativeBundleConfiguration)
        }
    }

    private fun processToolchain(
        bundleDir: File,
//        projectLogger: Logger,
        reinstallFlag: Boolean,
        kotlinNativeVersion: String,
        kotlinNativeBundleConfiguration: ConfigurableFileCollection,
    ) {
        val lock =
            NativeDistributionCommonizerLock(bundleDir) { message -> println("Kotlin Native Bundle: $message") }

        lock.withLock {
            val needToReinstall = false
//                KotlinToolingVersion(parameters.kotlinNativeVersion.get()).maturity == KotlinToolingVersion.Maturity.SNAPSHOT
            if (needToReinstall) {
                println("Snapshot version could be changed, to be sure that up-to-date version is used, Kotlin/Native should be reinstalled")
            }

            removeBundleIfNeeded(reinstallFlag || needToReinstall, bundleDir)

            if (!bundleDir.resolve(MARKER_FILE).exists()) {
                val gradleCachesKotlinNativeDir =
                    resolveKotlinNativeConfiguration(kotlinNativeVersion, kotlinNativeBundleConfiguration)

                println("Moving Kotlin/Native bundle from tmp directory $gradleCachesKotlinNativeDir to ${bundleDir.absolutePath}")
                gradleCachesKotlinNativeDir.walk().forEach { sourceFile ->
                    val relativePath = sourceFile.toRelativeString(gradleCachesKotlinNativeDir)
                    val bundleDirFile = bundleDir.resolve(relativePath)
                    when {
                        sourceFile.isFile -> copy(sourceFile.toPath(), bundleDirFile.toPath())
                        sourceFile.isDirectory -> bundleDirFile.mkdir()
                    }
                }

                createSuccessfulInstallationFile(bundleDir)
                println("Moved Kotlin/Native bundle from $gradleCachesKotlinNativeDir to ${bundleDir.absolutePath}")
            }
        }
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


    private fun createSuccessfulInstallationFile(bundleDir: File) {
        bundleDir.resolve(MARKER_FILE).createNewFile()
    }

    companion object {
        private var canBeReinstalled: Boolean = true // we can reinstall a k/n bundle once during the build
        private const val MARKER_FILE = "provisioned.ok"
    }
}