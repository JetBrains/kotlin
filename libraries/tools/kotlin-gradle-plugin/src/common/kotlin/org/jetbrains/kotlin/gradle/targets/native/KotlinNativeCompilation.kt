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
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
abstract class AbstractKotlinNativeCompilation internal constructor(
    compilation: KotlinCompilationImpl,
    val konanTarget: KonanTarget
) : DeprecatedAbstractKotlinCompilation<KotlinCommonOptions>(compilation) {

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

    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override val compilerOptions: DeprecatedHasCompilerOptions<KotlinNativeCompilerOptions>
        get() = compilation.compilerOptions as DeprecatedHasCompilerOptions<KotlinNativeCompilerOptions>

    internal val useGenericPluginArtifact: Boolean
        get() = project.nativeUseEmbeddableCompilerJar
}

open class KotlinNativeCompilation @Inject internal constructor(
    konanTarget: KonanTarget, compilation: KotlinCompilationImpl,
) : AbstractKotlinNativeCompilation(compilation, konanTarget) {

    final override val target: KotlinNativeTarget
        get() = compilation.target as KotlinNativeTarget

    @Suppress("DEPRECATION")
    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
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
) : AbstractKotlinNativeCompilation(compilation, konanTargets.find { it.enabledOnCurrentHost } ?: konanTargets.first()),
    KotlinMetadataCompilation<KotlinCommonOptions> {
    override val target: KotlinMetadataTarget = compilation.target as KotlinMetadataTarget
}

internal val Project.nativeUseEmbeddableCompilerJar: Boolean
    get() = PropertiesProvider(this).nativeUseEmbeddableCompilerJar
