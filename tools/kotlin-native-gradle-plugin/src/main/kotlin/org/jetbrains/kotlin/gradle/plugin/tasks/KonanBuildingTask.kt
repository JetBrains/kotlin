package org.jetbrains.kotlin.gradle.plugin.tasks

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

    @Input
    var dumpParameters: Boolean = false

    @Input
    val extraOpts = mutableListOf<String>()

    val konanVersion
        @Input get() = project.konanVersion
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
