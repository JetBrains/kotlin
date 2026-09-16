/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.setExecutable
import java.io.File
import javax.inject.Inject
import kotlin.io.path.Path

@DisableCachingByDefault
@ExperimentalWasmDsl
abstract class WasmToolsSetupTask @Inject constructor(
    settings: WasmToolsEnvSpec,
) : AbstractSetupTask<WasmToolsEnv, WasmToolsEnvSpec>(settings) {

    @get:Internal
    override val artifactPattern: String
        get() = "v[revision]/[artifact]-[revision]-[classifier].[ext]"

    @get:Internal
    override val artifactModule: String
        get() = "bytecodealliance.wasm-tools"

    @get:Internal
    override val artifactName: String
        get() = "wasm-tools"

    private val isWindows = env.map { it.isWindows }

    private val executable = env.map { it.executable }

    override fun extract(archive: File) {
        val destination = destinationProvider.getFile()
        val isWindows = isWindows.get()

        fs.copy {
            it.from(
                if (isWindows) {
                    archiveOperations.zipTree(archive)
                } else {
                    archiveOperations.tarTree(archive)
                }
            )
            it.into(destination.parentFile)
        }

        if (!isWindows) {
            Path(executable.get()).setExecutable()
        }
    }

    companion object {
        @InternalKotlinGradlePluginApi
        const val BASE_NAME: String = "WasmToolsSetup"
    }
}
