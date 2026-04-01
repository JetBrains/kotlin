/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain.Companion.abiValidation
import org.jetbrains.kotlin.compilerRunner.btapi.BuildSessionService
import org.jetbrains.kotlin.gradle.internal.UsesClassLoadersCachingBuildService

/**
 * A parent class for all tasks that use Application Binary Interface (ABI) tools.
 */
@DisableCachingByDefault(because = "Abstract task")
internal abstract class AbiToolsTask : DefaultTask(), UsesClassLoadersCachingBuildService {
    @get:Internal
    abstract val buildSessionService: Property<BuildSessionService>

    @get:Classpath
    abstract val buildToolsClasspath: ConfigurableFileCollection

    @TaskAction
    fun execute() {
        val buildSession = buildSessionService.get().getOrCreateBuildSession(
            classLoadersCachingService.get(),
            buildToolsClasspath.toList()
        )
        runTools(buildSession.kotlinToolchains.abiValidation, buildSession)
    }

    protected abstract fun runTools(abiValidationToolchain: AbiValidationToolchain, buildSession: KotlinToolchains.BuildSession)


}
