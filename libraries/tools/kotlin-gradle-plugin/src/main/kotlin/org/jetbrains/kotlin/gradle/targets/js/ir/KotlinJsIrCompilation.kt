/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

class KotlinJsIrCompilation(
    target: KotlinTarget,
    name: String
) : KotlinJsCompilation(target, name) {
    val productionCompilation: Kotlin2JsCompile =
        (target.project.tasks.getByName(compileKotlinTaskName) as Kotlin2JsCompile)

    val developmentCompilation: Kotlin2JsCompile =
        (target.project.tasks.getByName(compileKotlinTaskName) as Kotlin2JsCompile)
}