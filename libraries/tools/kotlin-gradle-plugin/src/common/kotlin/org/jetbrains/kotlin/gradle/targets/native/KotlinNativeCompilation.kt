/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import javax.inject.Inject

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION_ERROR", "DEPRECATION")
abstract class AbstractKotlinNativeCompilation internal constructor(
    compilation: KotlinCompilationImpl,
    val konanTarget: KonanTarget,
) : DeprecatedAbstractKotlinCompilation<KotlinAnyOptionsDeprecated>(compilation) {

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "Accessing task instance directly is deprecated. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("compileTaskProvider"),
        level = DeprecationLevel.ERROR,
    )
    override val compileKotlinTask: KotlinNativeCompile
        get() = compilation.compileKotlinTask as KotlinNativeCompile

    @Suppress("UNCHECKED_CAST", "DEPRECATION_ERROR")
    @Deprecated(
        "Replaced with compileTaskProvider. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("compileTaskProvider"),
        level = DeprecationLevel.ERROR,
    )
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

    /**
     * Indicates whether cross-compilation is supported for the given binary's target.
     *
     * Cross-compilation is supported if the target is enabled by the host manager
     * or if none of the target's compilations involve C interop dependencies.
     */
    val crossCompilationSupported: Provider<Boolean> = project.provider {
        target.crossCompilationOnCurrentHostSupported.getOrThrow()
    }
}

@Suppress("DEPRECATION")
open class KotlinSharedNativeCompilation @Inject internal constructor(
    val konanTargets: List<KonanTarget>,
    compilation: KotlinCompilationImpl,
) : AbstractKotlinNativeCompilation(
    compilation,
    konanTargets.find { it.enabledOnCurrentHostForKlibCompilation(compilation.project.kotlinPropertiesProvider) } ?: konanTargets.first()
),
    KotlinMetadataCompilation<Any> {
    override val target: KotlinMetadataTarget = compilation.target as KotlinMetadataTarget
}
