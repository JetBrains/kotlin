/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

abstract class AbstractKotlinNativeCompilation internal constructor(
    compilation: KotlinCompilationImpl,
    konanTarget: KonanTarget,
) : AbstractKotlinCompilation<KotlinCommonOptions>(compilation) {

    // TODO(remove after review): a bit dirty implementation, should properly deprecate the property
    //   as per KT-64521
    open val konanTarget: KonanTarget = konanTarget

    @Suppress("DEPRECATION")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: KotlinNativeCompile
        get() = compilation.compileKotlinTask as KotlinNativeCompile

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    @Deprecated("Replaced with compileTaskProvider", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTaskProvider: TaskProvider<out KotlinNativeCompile>
        get() = compilation.compileKotlinTaskProvider as TaskProvider<out KotlinNativeCompile>

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<KotlinNativeCompile>
        get() = compilation.compileTaskProvider as TaskProvider<KotlinNativeCompile>

    @Suppress("UNCHECKED_CAST")
    override val compilerOptions: HasCompilerOptions<KotlinNativeCompilerOptions>
        get() = compilation.compilerOptions as HasCompilerOptions<KotlinNativeCompilerOptions>

    internal fun compilerOptions(configure: KotlinNativeCompilerOptions.() -> Unit) {
        compilerOptions.configure(configure)
    }

    internal fun compilerOptions(configure: Action<KotlinNativeCompilerOptions>) {
        configure.execute(compilerOptions.options)
    }

    internal val useGenericPluginArtifact: Boolean
        get() = project.nativeUseEmbeddableCompilerJar
}

open class KotlinNativeCompilation @Inject internal constructor(
    konanTarget: KonanTarget, compilation: KotlinCompilationImpl,
) : AbstractKotlinNativeCompilation(compilation, konanTarget) {

    final override val target: KotlinNativeTarget
        get() = compilation.target as KotlinNativeTarget

    override val compilerOptions: NativeCompilerOptions
        get() = super.compilerOptions as NativeCompilerOptions

    // Interop DSL.
    val cinterops = compilation.project.container(DefaultCInteropSettings::class.java, DefaultCInteropSettingsFactory(compilation))

    fun cinterops(action: Action<NamedDomainObjectContainer<DefaultCInteropSettings>>) = action.execute(cinterops)

    // Naming
    final override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    val binariesTaskName: String
        get() = lowerCamelCaseName(target.disambiguationClassifier, compilation.compilationName, "binaries")
}

open class KotlinSharedNativeCompilation @Inject internal constructor(
    val konanTargets: List<KonanTarget>,
    compilation: KotlinCompilationImpl
) : AbstractKotlinNativeCompilation(compilation, konanTargets.first()), KotlinMetadataCompilation<KotlinCommonOptions> {

    // TODO(won't be pushed):
    //   technical problem here, can't refer to `this.project` in super-constructor call, but can't remove `konanTarget` from it
    //   because of binary compatibility
    //   This is a dirty temporary implementation, it won't be pushed to master. Proper handling should be implemented first in KT-64521
    override val konanTarget: KonanTarget =
        konanTargets.find { it.enabledOnCurrentHostForKlibCompilation(project.kotlinPropertiesProvider) } ?: konanTargets.first()

    override val target: KotlinMetadataTarget = compilation.target as KotlinMetadataTarget
}

internal val Project.nativeUseEmbeddableCompilerJar: Boolean
    get() = PropertiesProvider(this).nativeUseEmbeddableCompilerJar
