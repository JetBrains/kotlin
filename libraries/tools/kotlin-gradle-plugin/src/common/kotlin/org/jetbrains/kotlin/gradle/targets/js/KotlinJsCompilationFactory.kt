/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation

class KotlinJsCompilationFactory(
    val project: Project,
    val target: KotlinOnlyTarget<KotlinJsCompilation>,
    val irTarget: KotlinOnlyTarget<KotlinJsIrCompilation>?
) : KotlinCompilationFactory<KotlinJsCompilation> {
    override val itemClass: Class<KotlinJsCompilation>
        get() = KotlinJsCompilation::class.java

    override fun create(name: String): KotlinJsCompilation =
        KotlinJsCompilation(target, name)
}