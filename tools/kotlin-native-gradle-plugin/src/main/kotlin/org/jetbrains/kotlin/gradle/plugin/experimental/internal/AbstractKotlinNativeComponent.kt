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
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.publish.maven.MavenPom
import org.gradle.language.internal.DefaultBinaryCollection
import org.gradle.language.nativeplatform.internal.ComponentWithNames
import org.gradle.language.nativeplatform.internal.DefaultNativeComponent
import org.gradle.language.nativeplatform.internal.Names
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeDependencies
import org.jetbrains.kotlin.gradle.plugin.experimental.TargetSettings
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetImpl
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

class TargetSettingsImpl(val konanTarget: KonanTarget) : Named, TargetSettings {
    override fun getName(): String = konanTarget.name
    override val linkerOpts = mutableListOf<String>()
    override fun linkerOpts(values: List<String>) = linkerOpts(*values.toTypedArray())
    override fun linkerOpts(vararg values: String) {
        linkerOpts.addAll(values)
    }
}

abstract class AbstractKotlinNativeComponent @Inject constructor(
        private val name: String,
        override val sources: KotlinNativeSourceSetImpl,
        val project: Project,
        val objectFactory: ObjectFactory,
        fileOperations: FileOperations
) : DefaultNativeComponent(objectFactory),
    KotlinNativeComponent,
    ComponentWithNames {

    private val baseName: Property<String> = objectFactory.property(String::class.java).apply { set(name) }
    fun getBaseName(): Property<String> = baseName

    override val konanTargets: SetProperty<KonanTarget> =
            objectFactory.setProperty(KonanTarget::class.java).apply {
                set(mutableSetOf(HostManager.host))
            }

    @Suppress("UNCHECKED_CAST")
    private val binaries = objectFactory.newInstance(DefaultBinaryCollection::class.java, KotlinNativeBinary::class.java)
            as DefaultBinaryCollection<KotlinNativeBinary>
    override fun getBinaries(): DefaultBinaryCollection<KotlinNativeBinary> = binaries

    override fun getName(): String = name

    private val names = Names.of(name)
    override fun getNames(): Names = names

    private val dependencies: KotlinNativeDependenciesImpl = objectFactory.newInstance(
        KotlinNativeDependenciesImpl::class.java,
        project,
        names.withSuffix("implementation"),
        names.withSuffix("export")
    )
    internal val poms = mutableListOf<Action<MavenPom>>()

    override fun getDependencies() = dependencies

    override fun getImplementationDependencies(): Configuration = dependencies.implementationDependencies

    val targetSettings =
        project.container(TargetSettingsImpl::class.java) { name ->
            TargetSettingsImpl(HostManager().targetByName(name))
        }

    // region DSL.

    override var targets: List<String>
        get() = konanTargets.get().map { it.name }
        set(value) {
            val hostManager = HostManager()
            konanTargets.set(value.map { hostManager.targetByName(it) })
        }

    private val String.canonicalTargetName: String
        get() = HostManager().targetByName(this).name

    override fun target(konanTarget: KonanTarget): TargetSettings = targetSettings.maybeCreate(konanTarget.name)
    override fun target(target: String): TargetSettings = targetSettings.maybeCreate(target.canonicalTargetName)

    override fun target(target: String, action: TargetSettings.() -> Unit) =
        targetSettings.maybeCreate(target.canonicalTargetName).action()

    override fun target(target: String, action: Closure<Unit>) =
        target(target, ConfigureUtil.configureUsing(action))

    override fun target(target: String, action: Action<TargetSettings>) =
        target(target) { action.execute(this) }

    override fun allTargets(action: TargetSettings.() -> Unit) = targetSettings.all(action)
    override fun allTargets(action: Closure<Unit>) = targetSettings.all(action)
    override fun allTargets(action: Action<TargetSettings>) = targetSettings.all(action)

    @Deprecated("Use the 'targets' property instead. E.g. targets = ['macos_x64', 'linux_x64']")
    override fun target(vararg targets: String) {
        project.logger.warn("""
            Kotlin/Native component's method 'target' is deprecated. Use the 'targets' property instead.
            E.g. targets = ['macos_x64', 'linux_x64']
        """.trimIndent())
        this.targets = targets.toList()
    }

    override val extraOpts = mutableListOf<String>()

    override fun extraOpts(vararg values: Any) = extraOpts(values.toList())
    override fun extraOpts(values: List<Any>) {
        extraOpts.addAll(values.map { it.toString() })
    }

    override fun pom(action: Action<MavenPom>) {
        poms.add(action)
    }

    override var publishJavadoc: Boolean = true
    override var publishSources: Boolean = true

    override fun dependencies(action: KotlinNativeDependencies.() -> Unit) {
        dependencies.action()
    }

    override fun dependencies(action: Closure<Unit>) =
        dependencies(ConfigureUtil.configureUsing(action))

    override fun dependencies(action: Action<KotlinNativeDependencies>) {
        action.execute(dependencies)
    }

    override var entryPoint: String? = null
    override fun entryPoint(value: String) { entryPoint = value }

    // endregion
}