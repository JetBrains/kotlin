/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * A [WasmToolsToolchainService] that disables wasm-tools toolchain.
 *
 * Without a wasm-tools toolchain, wasm-tools is configured and provisioned per project, using the wasm-tools environment spec
 * and the wasm-tools setup task, instead of Gradle toolchain resolution:
 *
 * - If `download` is enabled, the setup task resolves the wasm-tools distribution for the current platform and architecture
 *   as an `org.wasmtools:wasm-tools` dependency from a project-level configuration and repository,
 *   unpacks it into the installation directory, and the `wasm-tools` executable from there is used.
 * - If `download` is disabled, no distribution is downloaded;
 *   the wasm-tools executable already installed on the machine is used (the `command` from the spec, `wasm-tools` by default).
 * - Provisioning is not shared between builds or projects through the toolchain mechanism;
 *   each project resolves its wasm-tools through its own configuration and repositories.
 *
 * Selected with [WasmToolsToolchainManagementDsl.disable].
 */
@ExperimentalWasmDsl
interface DisabledWasmToolsToolchainService : WasmToolsToolchainService<WasmToolsToolchainService.Parameters>
