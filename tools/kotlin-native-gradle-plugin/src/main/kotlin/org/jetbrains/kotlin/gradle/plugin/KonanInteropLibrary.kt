package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanInteropTask
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

open class KonanInteropLibrary(name: String, project: ProjectInternal, instantiator: Instantiator)
    : KonanBuildingConfig<KonanInteropTask>(name, KonanInteropTask::class.java, project, instantiator), KonanInteropSpec {

    override fun generateTaskDescription(task: KonanInteropTask) =
            "Build the Kotlin/Native interop library '${task.name}' for target '${task.target}'"

    override fun generateAggregateTaskDescription(task: Task) =
            "Build the Kotlin/Native interop library '${task.name}' for all supported and declared targets'"

    override fun generateHostTaskDescription(task: Task, hostTarget: KonanTarget) =
            "Build the Kotlin/Native interop library '${task.name}' for current host"

    override val defaultOutputDir: File
        get() = project.konanLibsOutputDir

    // DSL

    override fun defFile(file: Any) = forEach { it.defFile(file) }

    override fun pkg(value: String) = forEach { it.pkg(value) }

    override fun compilerOpts(vararg values: String) = forEach { it.compilerOpts(*values) }

    override fun headers(vararg files: Any) = forEach { it.headers(*files) }

    override fun headers(files: FileCollection) = forEach { it.headers(files) }

    override fun includeDirs(vararg values: Any) = forEach { it.includeDirs(*values) }

    override fun linkerOpts(values: List<String>) = forEach { it.linkerOpts(values) }

    override fun link(vararg files: Any) = forEach { it.link(*files) }
    override fun link(files: FileCollection) = forEach { it.link(files) }
}