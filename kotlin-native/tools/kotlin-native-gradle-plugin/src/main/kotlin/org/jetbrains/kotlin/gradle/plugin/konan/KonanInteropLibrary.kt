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

package org.jetbrains.kotlin.gradle.plugin.konan

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.util.ConfigureUtil
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
            it.configure { t -> t.includeDirs.allHeaders(includeDirs) }
        }

        override fun headerFilterOnly(vararg includeDirs: Any) = headerFilterOnly(includeDirs.toList())
        override fun headerFilterOnly(includeDirs: Collection<Any>) = tasks().forEach {
            it.configure { t -> t.includeDirs.headerFilterOnly(includeDirs) }
        }
    }

    val includeDirs = IncludeDirectoriesSpecImpl()

    override fun defFile(file: Any) = tasks().forEach { it.configure { t -> t.defFile(file) } }

    override fun packageName(value: String) = tasks().forEach { it.configure { t -> t.packageName(value) } }

    override fun compilerOpts(vararg values: String) = tasks().forEach { it.configure { t -> t.compilerOpts(*values) } }

    override fun headers(vararg files: Any) = tasks().forEach { it.configure { t -> t.headers(*files) } }

    override fun headers(files: FileCollection) = tasks().forEach { it.configure { t -> t.headers(files) } }

    override fun includeDirs(vararg values: Any) = tasks().forEach { it.configure { t -> t.includeDirs(*values) } }
    override fun includeDirs(closure: Closure<Unit>) = includeDirs(ConfigureUtil.configureUsing(closure))
    override fun includeDirs(action: Action<IncludeDirectoriesSpec>) = includeDirs { action.execute(this) }
    override fun includeDirs(configure: IncludeDirectoriesSpec.() -> Unit) = includeDirs.configure()

    override fun linkerOpts(values: List<String>) = tasks().forEach { it.configure { t -> t.linkerOpts(values) } }
    override fun linkerOpts(vararg values: String) = linkerOpts(values.toList())

    override fun link(vararg files: Any) = tasks().forEach { it.configure { t -> t.link(*files) } }
    override fun link(files: FileCollection) = tasks().forEach { it.configure { t -> t.link(files) } }
    override fun dependencies(closure: Closure<Unit>) = tasks().forEach { it.configure { t -> t.dependencies(closure) }}
}