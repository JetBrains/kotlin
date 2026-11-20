/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterFlavor
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterVersion
import java.io.File
import java.util.Locale

@OptIn(ExperimentalWasmDsl::class)
internal val WasiAdapterVersion.normalized: String
    get() = version.removePrefix("v")

@OptIn(ExperimentalWasmDsl::class)
internal fun wasiAdapterFileName(flavor: WasiAdapterFlavor): String =
    "wasi_snapshot_preview1.${flavor.name.lowercase(Locale.ROOT)}.wasm"

@OptIn(ExperimentalWasmDsl::class)
internal fun wasiAdapterDirectory(
    installationsDir: File,
    version: WasiAdapterVersion,
): File = installationsDir.resolve("v${version.normalized}")

@OptIn(ExperimentalWasmDsl::class)
internal fun wasiAdapterFile(
    installationsDir: File,
    version: WasiAdapterVersion,
    flavor: WasiAdapterFlavor,
): File = wasiAdapterDirectory(installationsDir, version).resolve(wasiAdapterFileName(flavor))
