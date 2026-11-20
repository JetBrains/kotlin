/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl

/**
 * A [BinaryenToolchainService] that uses a pre-installed Binaryen distribution instead of downloading one.
 *
 * The Binaryen installation is not provisioned; it must be available on the machine that runs the build.
 */
@ExperimentalExternalToolchainDsl
interface PreInstalledBinaryenToolchainService : BinaryenToolchainService<PreInstalledBinaryenToolchainService.Parameters> {
    abstract class Parameters : BuildServiceParameters {
        /** Required root directory of the Binaryen distribution, containing its `bin` directory. */
        abstract val toolchainDir: DirectoryProperty
    }
}
