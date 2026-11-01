/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl

/**
 * The default [BinaryenToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested Binaryen distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
@ExperimentalExternalToolchainDsl
interface DefaultBinaryenToolchainService : BinaryenToolchainService<DefaultBinaryenToolchainService.Parameters> {
    /** Configuration data for downloading Binaryen; the service creates its provisioner lazily. */
    abstract class Parameters : BuildServiceParameters {
        /** Directory containing Binaryen installations, defaulting to `<user.home>/.kotlin/toolchains/binaryen`. */
        abstract val installationDir: DirectoryProperty

        /** Base URL of the Binaryen release archives. */
        abstract val downloadBaseUrl: Property<String>

        /** Whether only existing installations may be used, defaulting to Gradle's `--offline` setting. */
        abstract val offline: Property<Boolean>
    }
}
