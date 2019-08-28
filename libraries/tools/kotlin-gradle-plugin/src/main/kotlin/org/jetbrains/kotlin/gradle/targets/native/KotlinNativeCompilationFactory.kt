/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project

class KotlinNativeCompilationFactory(
    val project: Project,
    val target: KotlinNativeTarget
) : KotlinCompilationFactory<KotlinNativeCompilation> {

    override val itemClass: Class<KotlinNativeCompilation>
        get() = KotlinNativeCompilation::class.java

    override fun create(name: String): KotlinNativeCompilation =
        // TODO: Validate compilation free args using the [CompilationFreeArgsValidator]
        //       when the compilation and the link args are separated (see KT-33717).
        // Note: such validation should be done in the whenEvaluate block because
        // a user can change args during project configuration.
        KotlinNativeCompilation(target, target.konanTarget, name)
}