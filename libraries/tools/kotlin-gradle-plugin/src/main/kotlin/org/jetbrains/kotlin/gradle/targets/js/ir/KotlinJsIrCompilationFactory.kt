/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget

class KotlinJsIrCompilationFactory(
    val project: Project,
    val target: KotlinOnlyTarget<KotlinJsIrCompilation>
) : KotlinCompilationFactory<KotlinJsIrCompilation> {
    override val itemClass: Class<KotlinJsIrCompilation>
        get() = KotlinJsIrCompilation::class.java

    override fun create(name: String): KotlinJsIrCompilation =
        KotlinJsIrCompilation(target, name)
}