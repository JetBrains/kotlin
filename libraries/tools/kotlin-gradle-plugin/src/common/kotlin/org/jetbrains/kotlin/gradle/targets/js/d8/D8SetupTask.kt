/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin.Companion.kotlinD8Extension
import java.io.File

@DisableCachingByDefault
abstract class D8SetupTask : AbstractSetupTask<D8Env, D8RootExtension>() {
    @Transient
    @Internal
    override val settings = project.kotlinD8Extension

    @get:Internal
    override val artifactPattern: String
        get() = "[artifact]-[revision].[ext]"

    @get:Internal
    override val artifactModule: String
        get() = "google.d8"

    @get:Internal
    override val artifactName: String
        get() = "v8"

    override fun extract(archive: File) {
        fs.copy {
            it.from(archiveOperations.zipTree(archive))
            //
            it.into(destination)
        }

        if (!env.isWindows) {
            File(env.executable).setExecutable(true)
        }
    }

    companion object {
        const val NAME: String = "kotlinD8Setup"
    }
}
