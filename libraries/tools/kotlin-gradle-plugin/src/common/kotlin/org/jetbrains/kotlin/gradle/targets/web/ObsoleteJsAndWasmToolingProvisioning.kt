/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web

import org.jetbrains.annotations.ApiStatus

/**
 * Marks the per-project provisioning of JS and Wasm tools (Node.js, D8, Binaryen, Swc, Wasmtime):
 * tools downloaded and configured through an `EnvSpec` extension and a setup task.
 *
 * Per-project provisioning breaks Isolated Projects, is hard to point at pre-installed distributions,
 * and is incompatible with Gradle's centralized repository declarations.
 * It is being replaced by toolchain services, like
 * [org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainService] (KT-80311, KT-88733).
 */
internal typealias ObsoleteJsAndWasmToolingProvisioning = ApiStatus.Obsolete
