/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtime

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
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
abstract class WasmtimeSetupTask @Inject constructor(
    settings: WasmtimeEnvSpec,
) : AbstractSetupTask<WasmtimeEnv, WasmtimeEnvSpec>(settings) {

    @get:Internal
    override val artifactPattern: String
        get() = "v[revision]/[artifact]-v[revision]-[classifier].[ext]"

    @get:Internal
    override val artifactModule: String
        get() = "bytecodealliance.wasmtime"

    @get:Internal
    override val artifactName: String
        get() = "wasmtime"

    private val isWindows = env.map { it.isWindows }

    private val executable = env.map { it.executable }

    override fun extract(archive: File) {
        val destination = destinationProvider.getFile()
        val isWindows = isWindows.get()

        // Repack .tar.xz archives into .tar so that Gradle can read them,
        // see https://github.com/gradle/gradle/issues/31858
        val source = if (!isWindows) {
            val tarFile = temporaryDir.resolve(archive.nameWithoutExtension)
            XZCompressorInputStream(archive.inputStream().buffered()).use { xzIn ->
                tarFile.outputStream().buffered().use { tarOut ->
                    xzIn.copyTo(tarOut)
                }
            }
            tarFile
        } else {
            archive
        }

        fs.copy {
            it.from(
                if (isWindows) {
                    archiveOperations.zipTree(source)
                } else {
                    archiveOperations.tarTree(source)
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
        const val BASE_NAME: String = "WasmtimeSetup"
    }
}
