/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.AbstractSetupTask
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class SwcSetupTask
@Inject
internal constructor(
    settings: SwcEnvSpec,
) : AbstractSetupTask<SwcEnv, SwcEnvSpec>(settings) {

    @get:Internal
    override val artifactPattern: String
        get() = "v[revision]/[artifact]-[classifier]"

    @get:Internal
    override val artifactModule: String
        get() = "com.github.swc-project"

    @get:Internal
    override val artifactName: String
        get() = "swc"

    private val executable = env.map { it.executable }

    override fun extract(archive: File) {
        fs.copy {
            it.from(archive)
            it.into(destinationProvider.getFile())
        }

        File(executable.get()).setExecutable(true)
    }

    companion object {
        @InternalKotlinGradlePluginApi
        const val BASE_NAME: String = "swcSetup"
    }
}