/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootPlugin.Companion.kotlinBinaryenExtension
import java.io.File

@DisableCachingByDefault
abstract class BinaryenSetupTask : AbstractSetupTask<BinaryenEnv, BinaryenRootExtension>() {
    @Transient
    @Internal
    override val settings = project.kotlinBinaryenExtension

    @get:Internal
    override val artifactPattern: String
        get() = "version_[revision]/binaryen-version_[revision]-[classifier].[ext]"

    @get:Internal
    override val artifactModule: String
        get() = "com.github.webassembly"

    @get:Internal
    override val artifactName: String
        get() = "binaryen"

    override fun extract(archive: File) {
        fs.copy {
            it.from(archiveOperations.tarTree(archive))
            it.into(destination.parentFile)
        }

        if (!env.isWindows) {
            File(env.executable).setExecutable(true)
        }
    }

    companion object {
        const val NAME: String = "kotlinBinaryenSetup"
    }
}
