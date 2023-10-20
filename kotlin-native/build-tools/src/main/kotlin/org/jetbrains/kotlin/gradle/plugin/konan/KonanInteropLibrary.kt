/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.konan.KonanInteropSpec.IncludeDirectoriesSpec
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanInteropTask
import java.io.File

open class KonanInteropLibrary(name: String,
                               project: ProjectInternal,
                               targets: Iterable<String> = project.konanExtension.targets
) : KonanBuildingConfig<KonanInteropTask>(name, KonanInteropTask::class.java, project, targets),
        KonanInteropSpec
{

    override fun generateTaskDescription(task: KonanInteropTask) =
            "Build the Kotlin/Native interop library '${task.name}' for target '${task.konanTarget}'"

    override fun generateAggregateTaskDescription(task: Task) =
            "Build the Kotlin/Native interop library '${task.name}' for all supported and declared targets'"

    override fun generateTargetAliasTaskDescription(task: Task, targetName: String) =
            "Build the Kotlin/Native interop library '${task.name}' for '$targetName'"

    override val defaultBaseDir: File
        get() = project.konanLibsBaseDir

    // DSL

    inner class IncludeDirectoriesSpecImpl: IncludeDirectoriesSpec {
        override fun allHeaders(vararg includeDirs: Any) = allHeaders(includeDirs.toList())
        override fun allHeaders(includeDirs: Collection<Any>) = tasks().forEach {
            it.configure { this@configure.includeDirs.allHeaders(includeDirs) }
        }

        override fun headerFilterOnly(vararg includeDirs: Any) = headerFilterOnly(includeDirs.toList())
        override fun headerFilterOnly(includeDirs: Collection<Any>) = tasks().forEach {
            it.configure { this@configure.includeDirs.headerFilterOnly(includeDirs) }
        }
    }

    val includeDirs = IncludeDirectoriesSpecImpl()

    override fun defFile(file: Any) = tasks().forEach { it.configure { defFile(file) } }

    override fun packageName(value: String) = tasks().forEach { it.configure { packageName(value) } }

    override fun compilerOpts(vararg values: String) = tasks().forEach { it.configure { compilerOpts(*values) } }

    override fun headers(vararg files: Any) = tasks().forEach { it.configure { headers(*files) } }

    override fun headers(files: FileCollection) = tasks().forEach { it.configure { headers(files) } }

    override fun includeDirs(vararg values: Any) = tasks().forEach { it.configure { includeDirs(*values) } }
    override fun includeDirs(closure: Closure<Unit>) = includeDirs { project.configure(this, closure) }
    override fun includeDirs(action: Action<IncludeDirectoriesSpec>) = includeDirs { action.execute(this) }
    override fun includeDirs(configure: IncludeDirectoriesSpec.() -> Unit) = includeDirs.configure()

    override fun linkerOpts(values: List<String>) = tasks().forEach { it.configure { linkerOpts(values) } }
    override fun linkerOpts(vararg values: String) = linkerOpts(values.toList())

    override fun link(vararg files: Any) = tasks().forEach { it.configure { link(*files) } }
    override fun link(files: FileCollection) = tasks().forEach { it.configure { link(files) } }
    override fun dependencies(closure: Closure<Unit>) = tasks().forEach { it.configure { dependencies(closure) }}

    override fun noPack(flag: Boolean) = tasks().forEach { it.configure { noPack(flag) } }
}