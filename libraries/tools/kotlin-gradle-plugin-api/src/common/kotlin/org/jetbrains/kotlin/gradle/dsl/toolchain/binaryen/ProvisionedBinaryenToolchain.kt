/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen

import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import java.nio.file.Path

/** Represents Binaryen tools provisioned on local file system. */
@ExperimentalExternalToolchainDsl
class ProvisionedBinaryenToolchain(
    /** The path to `wasm-opt` in the provisioned Binaryen distribution. */
    val wasmOpt: Path,
)
