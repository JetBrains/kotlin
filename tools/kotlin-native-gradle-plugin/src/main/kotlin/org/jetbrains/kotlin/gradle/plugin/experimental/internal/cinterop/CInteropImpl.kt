/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.experimental.internal.cinterop

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.language.ComponentDependencies
import org.gradle.language.internal.DefaultComponentDependencies
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.experimental.CInterop
import org.jetbrains.kotlin.gradle.plugin.experimental.CInteropSettings
import org.jetbrains.kotlin.gradle.plugin.experimental.CInteropSettings.IncludeDirectories
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

open class CInteropImpl @Inject constructor(
    private val project: Project,
    private val name: String
): CInterop {

    private val String.konanTarget: KonanTarget
        get() = HostManager().targetByName(this)

    override fun getName(): String = name

    private val platformSettings: DomainObjectCollection<CInteropSettingsImpl> =
        project.container(CInteropSettingsImpl::class.java)

    private val targetToSettings: MutableMap<KonanTarget, CInteropSettingsImpl> = mutableMapOf()

    // DSL.

    override val dependencies =
        DefaultComponentDependencies(project.configurations, name + "InteropImplementation")

    override fun target(target: String): CInteropSettings = target(target.konanTarget)

    override fun target(target: KonanTarget): CInteropSettings =
        targetToSettings.getOrPut(target) {
            CInteropSettingsImpl(project, name, target).also {
                platformSettings.add(it)
                it.dependencies.implementationDependencies.extendsFrom(
                    this.dependencies.implementationDependencies
                )
            }
        }

    override fun target(target: String, action: CInteropSettings.() -> Unit) = target(target.konanTarget, action)
    override fun target(target: String, action: Closure<Unit>) = target(target.konanTarget, action)
    override fun target(target: String, action: Action<CInteropSettings>) = target(target.konanTarget, action)

    override fun target(target: KonanTarget, action: CInteropSettings.() -> Unit) =
        target(target).action()

    override fun target(target: KonanTarget, action: Closure<Unit>) =
        target(target, ConfigureUtil.configureUsing(action))

    override fun target(target: KonanTarget, action: Action<CInteropSettings>) {
        action.execute(target(target))
    }

    override fun defFile(file: Any) = platformSettings.all { it.defFile(file) }

    override fun packageName(value: String) = platformSettings.all { it.packageName(value) }

    override fun headers(vararg files: Any) = platformSettings.all { it.headers(*files) }
    override fun headers(files: FileCollection) = platformSettings.all { it.headers(files) }

    override fun includeDirs(vararg values: Any) =
        platformSettings.all {it.includeDirs(*values) }
    override fun includeDirs(closure: Closure<Unit>) =
        platformSettings.all { it.includeDirs(closure) }
    override fun includeDirs(action: Action<IncludeDirectories>) =
        platformSettings.all { it.includeDirs(action) }
    override fun includeDirs(configure: IncludeDirectories.() -> Unit) =
        platformSettings.all { it.includeDirs(configure) }

    override fun compilerOpts(vararg values: String) = platformSettings.all { it.compilerOpts(*values) }
    override fun compilerOpts(values: List<String>) = platformSettings.all { it.compilerOpts(values) }

    override fun linkerOpts(vararg values: String) = platformSettings.all { it.linkerOpts(*values) }
    override fun linkerOpts(values: List<String>) = platformSettings.all { it.linkerOpts(values) }

    override fun extraOpts(vararg values: Any) = platformSettings.all { it.extraOpts(*values) }
    override fun extraOpts(values: List<Any>) = platformSettings.all { it.extraOpts(values) }

    override fun dependencies(action: ComponentDependencies.() -> Unit) {
        dependencies.action()
    }
    override fun dependencies(action: Closure<Unit>) =
        dependencies(ConfigureUtil.configureUsing(action))
    override fun dependencies(action: Action<ComponentDependencies>) {
        action.execute(dependencies)
    }
}