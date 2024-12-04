/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSourceSetInclusion
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinNativeCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinNativeCompilerOptionsFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.NativeKotlinCompilationDependencyConfigurationsFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.NativeKotlinCompilationTaskNamesContainerFactory

open class KotlinNativeCompilationFactory internal constructor(
    final override val target: KotlinNativeTarget
) : KotlinCompilationFactory<KotlinNativeCompilation> {

    override val itemClass: Class<KotlinNativeCompilation>
        get() = KotlinNativeCompilation::class.java

    private val compilationImplFactory: KotlinCompilationImplFactory =
        KotlinCompilationImplFactory(
            compilationTaskNamesContainerFactory = NativeKotlinCompilationTaskNamesContainerFactory,
            compilationDependencyConfigurationsFactory = NativeKotlinCompilationDependencyConfigurationsFactory,
            compilerOptionsFactory = KotlinNativeCompilerOptionsFactory,
            compilationAssociator = KotlinNativeCompilationAssociator,
            compilationSourceSetInclusion = KotlinCompilationSourceSetInclusion(
                KotlinCompilationSourceSetInclusion.NativeAddSourcesToCompileTask
            ),
        )

    override fun create(name: String): KotlinNativeCompilation {
        return target.project.objects.newInstance(
            KotlinNativeCompilation::class.java, target.konanTarget, compilationImplFactory.create(target, name)
        )
    }
}
