/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.wasmtools

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsRequest
import org.jetbrains.kotlin.gradle.tasks.wasmtools.UsesWasmToolsToolchainService
import java.io.File

@OptIn(ExperimentalWasmDsl::class)
abstract class WasmToolsServiceProvisioner : DefaultTask(), UsesWasmToolsToolchainService {

    @get:Nested
    abstract val wasmToolsRequest: Property<WasmToolsRequest>

    @get:OutputFile
    val executable: Provider<File> =
        wasmToolsToolchainService.zip(wasmToolsRequest) { service, request ->
            File(service.request(request).get().executable.get())
        }
}
