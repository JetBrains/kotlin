/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions
import org.jetbrains.kotlin.konan.target.KonanTarget

open class KotlinNativeCompilationFactory(
    override val target: KotlinNativeTarget
) : KotlinCompilationFactory<KotlinNativeCompilation> {

    override val itemClass: Class<KotlinNativeCompilation>
        get() = KotlinNativeCompilation::class.java

    @Suppress("DEPRECATION")
    override fun create(name: String): KotlinNativeCompilation {
        // TODO: Validate compilation free args using the [CompilationFreeArgsValidator]
        //       when the compilation and the link args are separated (see KT-33717).
        // Note: such validation should be done in the whenEvaluate block because
        // a user can change args during project configuration.

        val defaultSourceSet = getOrCreateDefaultSourceSet(name)
        return target.project.objects.newInstance(
            KotlinNativeCompilation::class.java,
            target.konanTarget,
            NativeCompilationDetails(
                target,
                name,
                defaultSourceSet,
                {
                    NativeCompilerOptions(
                        target.project,
                        defaultSourceSet.languageSettings
                    )
                },
                {
                    object : KotlinCommonOptions {
                        override val options: KotlinCommonCompilerOptions
                            get() = compilerOptions.options
                    }
                }
            )
        )
    }
}

class KotlinSharedNativeCompilationFactory(
    override val target: KotlinMetadataTarget,
    val konanTargets: List<KonanTarget>
) : KotlinCompilationFactory<KotlinSharedNativeCompilation> {
    override val itemClass: Class<KotlinSharedNativeCompilation>
        get() = KotlinSharedNativeCompilation::class.java

    override fun create(name: String): KotlinSharedNativeCompilation {
        val defaultSourceSet = getOrCreateDefaultSourceSet(name)
        return target.project.objects.newInstance(
            KotlinSharedNativeCompilation::class.java,
            konanTargets,
            SharedNativeCompilationDetails(
                target,
                name,
                defaultSourceSet,
                {
                    NativeCompilerOptions(
                        target.project,
                        defaultSourceSet.languageSettings
                    )
                },
                {
                    object : KotlinCommonOptions {
                        override val options: KotlinCommonCompilerOptions
                            get() = compilerOptions.options
                    }
                }
            )
        )
    }
}