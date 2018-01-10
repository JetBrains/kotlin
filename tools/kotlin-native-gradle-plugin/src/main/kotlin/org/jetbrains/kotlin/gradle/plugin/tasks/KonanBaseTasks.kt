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

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KonanArtifactSpec
import org.jetbrains.kotlin.gradle.plugin.KonanArtifactWithLibrariesSpec
import org.jetbrains.kotlin.gradle.plugin.KonanLibrariesSpec
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.TargetManager
import org.jetbrains.kotlin.konan.util.visibleName
import java.io.File

internal val Project.host
    get() = TargetManager.host.visibleName

internal val Project.simpleOsName
    get() = TargetManager.simpleOsName()

/** A task with a KonanTarget specified. */
abstract class KonanTargetableTask: DefaultTask() {

    @Input internal lateinit var konanTarget: KonanTarget

    internal open fun init(target: KonanTarget) {
        this.konanTarget = target
    }

    val targetIsSupported: Boolean
        @Internal get() = konanTarget.enabled

    val isCrossCompile: Boolean
        @Internal get() = (konanTarget != TargetManager.host)

    val target: String
        @Internal get() = konanTarget.visibleName
}

/** A task building an artifact. */
abstract class KonanArtifactTask: KonanTargetableTask(), KonanArtifactSpec {

    open val artifact: File
        @OutputFile get() = destinationDir.resolve(artifactFullName)

    @Internal lateinit var destinationDir: File
    @Internal lateinit var artifactName: String

    protected val artifactFullName: String
        @Internal get() = "$artifactPrefix$artifactName$artifactSuffix"

    val artifactPath: String
        @Internal get() = artifact.canonicalPath

    protected abstract val artifactSuffix: String
        @Internal get

    protected abstract val artifactPrefix: String
        @Internal get

    internal open fun init(destinationDir: File, artifactName: String, target: KonanTarget) {
        super.init(target)
        this.destinationDir = destinationDir
        this.artifactName = artifactName
    }

    // DSL.

    override fun artifactName(name: String) {
        artifactName = name
    }

    fun destinationDir(dir: Any) {
        destinationDir = project.file(dir)
    }
}

/** Task building an artifact with libraries */
abstract class KonanArtifactWithLibrariesTask: KonanArtifactTask(), KonanArtifactWithLibrariesSpec {
    @Nested
    val libraries = KonanLibrariesSpec(this, project)

    @Input
    var noDefaultLibs = false

    // DSL

    override fun libraries(closure: Closure<Unit>) = libraries(ConfigureUtil.configureUsing(closure))
    override fun libraries(action: Action<KonanLibrariesSpec>) = libraries { action.execute(this) }
    override fun libraries(configure: KonanLibrariesSpec.() -> Unit) { libraries.configure() }

    override fun noDefaultLibs(flag: Boolean) {
        noDefaultLibs = flag
    }
}
