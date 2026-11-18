/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.slf4j.LoggerFactory
import java.io.File

internal abstract class NativeVersionValueSource :
    ValueSource<String, NativeVersionValueSource.Params> {

    interface Params : ValueSourceParameters {
        val bundleDirectory: Property<String>
        val reinstallBundle: Property<Boolean>
        val simpleKotlinNativeVersion: Property<String>
        val kotlinNativeVersion: Property<String>
        val kotlinNativeCompilerConfiguration: Property<ConfigurableFileCollection>
    }

    override fun obtain(): String {
        val kotlinNativeVersion = parameters.kotlinNativeVersion.get()
        val bundleDir = File(parameters.bundleDirectory.get())
        val installer = KotlinNativeDistributionInstaller()

        val needToReinstall = isSnapshotVersion(parameters.simpleKotlinNativeVersion.get())
        if (needToReinstall) {
            logger.debug("Snapshot version could be changed, to be sure that up-to-date version is used, Kotlin/Native should be reinstalled")
        }
        if ((parameters.reinstallBundle.get() || needToReinstall) && canBeReinstalled) {
            logger.info("Removing Kotlin/Native bundle")
            installer.remove(bundleDir)
            canBeReinstalled = false // we don't need to reinstall k/n if it was reinstalled once during the same build
        }

        // The archive is resolved, which may download it, only by the build that unpacks it.
        installer.install(bundleDir) {
            resolveKotlinNativeConfiguration(kotlinNativeVersion, parameters.kotlinNativeCompilerConfiguration.get())
        }
        return kotlinNativeVersion
    }

    private fun resolveKotlinNativeConfiguration(
        kotlinNativeVersion: String,
        kotlinNativeCompilerConfiguration: ConfigurableFileCollection,
    ): File {
        val resolutionErrorMessage = "Kotlin Native dependency has not been properly resolved. " +
                "Please, make sure that you've declared the repository, which contains $kotlinNativeVersion."

        val gradleCachesKotlinNativeDir = kotlinNativeCompilerConfiguration
            .singleOrNull() ?: error(resolutionErrorMessage)

        return gradleCachesKotlinNativeDir
    }

    companion object {
        private var canBeReinstalled: Boolean = true // we can reinstall a k/n bundle once during the build
        val logger = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.targets.native.toolchain")
        internal fun isSnapshotVersion(kotlinNativeVersion: String): Boolean =
            KotlinToolingVersion(kotlinNativeVersion).maturity == KotlinToolingVersion.Maturity.SNAPSHOT
    }
}
