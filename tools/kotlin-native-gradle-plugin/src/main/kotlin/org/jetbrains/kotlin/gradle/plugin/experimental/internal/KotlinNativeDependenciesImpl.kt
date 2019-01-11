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

package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.language.internal.DefaultComponentDependencies
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.experimental.CInterop
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeDependencies
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.cinterop.CInteropImpl
import javax.inject.Inject

open class KotlinNativeDependenciesImpl @Inject constructor(
    private val project: Project,
    configurations: ConfigurationContainer,
    implementationName: String,
    exportName: String
) : DefaultComponentDependencies(configurations, implementationName),
    KotlinNativeDependencies {

    internal val exportDependencies = configurations.create(exportName).apply {
        isCanBeConsumed = false
        isCanBeResolved = false
        isTransitive = false
        implementationDependencies.extendsFrom(this)
    }

    override var transitiveExport: Boolean = false

    override fun export(notation: Any) {
        exportDependencies.dependencies.add(dependencyHandler.create(notation))
    }

    override fun export(notation: Any, configure: Closure<*>) {
        val dependency = dependencyHandler.create(notation)
        ConfigureUtil.configure(configure, dependency)
        exportDependencies.dependencies.add(dependency)
    }

    override fun export(notation: Any, configure: Action<in Dependency>) {
        val dependency = dependencyHandler.create(notation)
        configure.execute(dependency)
        exportDependencies.dependencies.add(dependency)
    }

    override val cinterops = project.container(CInteropImpl::class.java) { name ->
        CInteropImpl(project, name).apply {
            dependencies.implementationDependencies.extendsFrom(
                this@KotlinNativeDependenciesImpl.implementationDependencies
            )
        }
    }

    override fun cinterop(name: String) = cinterops.maybeCreate(name)

    override fun cinterop(name: String, action: CInterop.() -> Unit) {
        cinterop(name).apply { action() }
    }

    override fun cinterop(name: String, action: Closure<Unit>) {
        cinterop(name, ConfigureUtil.configureUsing(action))
    }

    override fun cinterop(name: String, action: Action<CInterop>) {
        cinterop(name).apply { action.execute(this) }
    }
}