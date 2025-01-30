/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalWasmDsl::class)
@DisableCachingByDefault
abstract class BinaryenSetupTask
@Inject
@InternalKotlinGradlePluginApi
constructor(
    settings: BinaryenEnvSpec,
) : AbstractSetupTask<BinaryenEnv, BinaryenEnvSpec>(settings) {

    @get:Internal
    override val artifactPattern: String
        get() = "version_[revision]/binaryen-version_[revision]-[classifier].[ext]"

    @get:Internal
    override val artifactModule: String
        get() = "com.github.webassembly"

    @get:Internal
    override val artifactName: String
        get() = "binaryen"

    private val isWindows = env.map { it.isWindows }

    private val executable = env.map { it.executable }

    override fun extract(archive: File) {
        fs.copy {
            it.from(archiveOperations.tarTree(archive))
            it.into(destinationProvider.getFile().parentFile)
        }

        if (!isWindows.get()) {
            File(executable.get()).setExecutable(true)
        }
    }

    companion object {
        const val NAME: String = "kotlinBinaryenSetup"
    }
}
