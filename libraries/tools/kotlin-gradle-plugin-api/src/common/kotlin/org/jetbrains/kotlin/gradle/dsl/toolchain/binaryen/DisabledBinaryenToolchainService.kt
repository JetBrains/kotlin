/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen

import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl

/**
 * A [BinaryenToolchainService] that disables Binaryen toolchain provisioning.
 *
 * Without a Binaryen toolchain, Binaryen is configured and provisioned per project, using the Binaryen
 * environment spec and the Binaryen setup task, instead of the build-wide toolchain service.
 *
 * Selected with [BinaryenToolchainManagementDsl.disable].
 */
@ExperimentalExternalToolchainDsl
interface DisabledBinaryenToolchainService : BinaryenToolchainService<BuildServiceParameters.None>
