/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.tasks

import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import org.jetbrains.kotlin.dependsOnDist
import org.jetbrains.kotlin.gradle.plugin.konan.KonanBuildingConfig
import org.jetbrains.kotlin.gradle.plugin.konan.KonanBuildingSpec
import org.jetbrains.kotlin.gradle.plugin.konan.KonanToolRunner
import org.jetbrains.kotlin.gradle.plugin.konan.konanHome

/** Base class for both interop and compiler tasks. */
abstract class KonanBuildingTask: KonanArtifactWithLibrariesTask(), KonanBuildingSpec {

    @get:Internal
    internal abstract val toolRunner: KonanToolRunner

    override fun init(config: KonanBuildingConfig<*>, destinationDir: File, artifactName: String, target: KonanTarget) {
        super.init(config, destinationDir, artifactName, target)
    }

    @Console
    var dumpParameters: Boolean = false

    @Input
    val extraOpts = mutableListOf<String>()

    val konanHome
        @Input get() = project.konanHome

    @TaskAction
    abstract fun run()

    // DSL.

    override fun dumpParameters(flag: Boolean) {
        dumpParameters = flag
    }

    override fun extraOpts(vararg values: Any) = extraOpts(values.toList())
    override fun extraOpts(values: List<Any>) {
        extraOpts.addAll(values.map { it.toString() })
    }
}
