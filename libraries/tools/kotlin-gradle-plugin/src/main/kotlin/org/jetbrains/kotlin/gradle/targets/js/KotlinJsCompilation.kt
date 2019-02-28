 /*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

class KotlinJsCompilation(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilationToRunnableFiles<KotlinJsOptions>(target, name), KotlinCompilationWithResources<KotlinJsOptions> {
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    override val compileKotlinTask: Kotlin2JsCompile
        get() = super.compileKotlinTask as Kotlin2JsCompile
}