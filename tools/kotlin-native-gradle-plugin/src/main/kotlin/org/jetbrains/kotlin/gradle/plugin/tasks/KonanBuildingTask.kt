/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.tasks

import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/** Base class for both interop and compiler tasks. */
abstract class KonanBuildingTask: KonanArtifactWithLibrariesTask(), KonanBuildingSpec {

    internal abstract val toolRunner: KonanToolRunner

    override fun init(destinationDir: File, artifactName: String, target: KonanTarget) {
        dependsOn(project.konanCompilerDownloadTask)
        super.init(destinationDir, artifactName, target)
    }

    @Console
    var dumpParameters: Boolean = false

    @Input
    val extraOpts = mutableListOf<String>()

    val konanHome
        @Input get() = project.konanHome

    protected abstract fun buildArgs(): List<String>

    @TaskAction
    fun run() {
        destinationDir.mkdirs()
        if (dumpParameters) { dumpProperties(this) }
        toolRunner.run(buildArgs())
    }

    // DSL.

    override fun dumpParameters(flag: Boolean) {
        dumpParameters = flag
    }

    override fun extraOpts(vararg values: Any) = extraOpts(values.toList())
    override fun extraOpts(values: List<Any>) {
        extraOpts.addAll(values.map { it.toString() })
    }
}
