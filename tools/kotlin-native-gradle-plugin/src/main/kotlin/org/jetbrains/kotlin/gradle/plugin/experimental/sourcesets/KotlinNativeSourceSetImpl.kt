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

package org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.util.ConfigureUtil.configure
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeDependencies
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.AbstractKotlinNativeComponent
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

open class KotlinNativeSourceSetImpl @Inject constructor(
        private val name: String,
        val sourceDirectorySetFactory: SourceDirectorySetFactory,
        val project: ProjectInternal
) : KotlinNativeSourceSet {

    override lateinit var component: AbstractKotlinNativeComponent

    override val kotlin: SourceDirectorySet
        get() = nativeSources

    internal val commonSources: ConfigurableFileCollection = project.files()

    private val nativeSources: SourceDirectorySet = newSourceDirectorySet("kotlin")

    private val platformSources = mutableMapOf<KonanTarget, SourceDirectorySet>()

    private fun newSourceDirectorySet(name: String) = sourceDirectorySetFactory.create(name).apply {
        filter.include("**/*.kt")
    }

    override fun getPlatformSources(target: KonanTarget) = platformSources.getOrPut(target) {
        newSourceDirectorySet(target.name)
    }

    override fun getCommonMultiplatformSources(): FileCollection = commonSources
    override fun getCommonNativeSources(): SourceDirectorySet = nativeSources

    override fun getAllSources(target: KonanTarget): FileCollection =
        commonSources + nativeSources + getPlatformSources(target)

    override fun getName(): String = name

    // region DSL

    // Common source directory set configuration.
    override fun kotlin(configureClosure: Closure<*>) = apply { configure(configureClosure, nativeSources) }
    override fun kotlin(configureAction: Action<in SourceDirectorySet>) = apply { configureAction.execute(nativeSources) }
    override fun kotlin(configureLambda: SourceDirectorySet.() -> Unit) = apply { nativeSources.configureLambda() }

    // Configuration of the corresponding software component.
    override fun component(configureClosure: Closure<*>) = apply { configure(configureClosure, component) }
    override fun component(configureAction: Action<in AbstractKotlinNativeComponent>) =
            apply { configureAction.execute(component) }
    override fun component(configureLambda: AbstractKotlinNativeComponent.() -> Unit) =
            apply { component.configureLambda() }

    // Adding new targets and configuration of target-specific source directory sets.
    override fun target(target: String): SourceDirectorySet {
        val konanTarget = HostManager().targetByName(target)
        component.konanTargets.add(konanTarget)
        return getPlatformSources(konanTarget)
    }

    override fun target(targets: Iterable<String>) = target(targets) {}

    override fun target(targets: Iterable<String>, configureLambda: SourceDirectorySet.() -> Unit) = apply {
        targets.forEach { target(it).configureLambda() }
    }

    override fun target(targets: Iterable<String>, configureClosure: Closure<*>) =
            target(targets) { configure(configureClosure, this) }

    override fun target(targets: Iterable<String>, configureAction: Action<in SourceDirectorySet>) =
            target(targets) { configureAction.execute(this) }

    override fun target(vararg targets: String) = target(targets.toList())

    override fun target(vararg targets: String, configureLambda: SourceDirectorySet.() -> Unit) =
            target(targets.toList(), configureLambda)

    override fun target(vararg targets: String, configureClosure: Closure<*>) =
            target(targets.toList(), configureClosure)

    override fun target(vararg targets: String, configureAction: Action<in SourceDirectorySet>) =
            target(targets.toList(), configureAction)

    override val dependencies: KotlinNativeDependencies
            get() = component.dependencies

    override fun dependencies(action: KotlinNativeDependencies.() -> Unit) = component.dependencies(action)
    override fun dependencies(action: Closure<Unit>) = component.dependencies(action)
    override fun dependencies(action: Action<KotlinNativeDependencies>) = component.dependencies(action)

    // endregion
}