/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import org.jetbrains.kotlin.konan.target.Configurables
import org.jetbrains.kotlin.konan.target.WasmConfigurables
import java.io.File

/**
 * [Executor] that runs the wasm process via `d8`.
 *
 * NOTE: Apart from [ExecuteRequest.executableAbsolutePath] a JS launcher is required,
 *       it must be located in the same folder with the same name and `.js` suffix appended.
 *
 * @param configurables [Configurables] for the wasm target
 */
class WasmExecutor(
        private val configurables: WasmConfigurables,
) : Executor {
    private val hostExecutor: Executor = HostExecutor()

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        val absoluteTargetToolchain = configurables.absoluteTargetToolchain
        val workingDirectory = request.workingDirectory ?: File(request.executableAbsolutePath).parentFile
        val executable = request.executableAbsolutePath
        val launcherJs = "$executable.js"
        return hostExecutor.execute(request.copying {
            this.executableAbsolutePath = "$absoluteTargetToolchain/bin/d8"
            this.workingDirectory = workingDirectory
            this.args.addAll(0, listOf("--expose-wasm", launcherJs, "--", executable))
        })
    }
}