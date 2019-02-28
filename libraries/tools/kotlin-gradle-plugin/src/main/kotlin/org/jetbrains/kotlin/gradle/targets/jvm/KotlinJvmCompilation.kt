 /*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

open class KotlinJvmCompilation(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilationToRunnableFiles<KotlinJvmOptions>(target, name), KotlinCompilationWithResources<KotlinJvmOptions> {
    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    override val compileKotlinTask: org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        get() = super.compileKotlinTask as org.jetbrains.kotlin.gradle.tasks.KotlinCompile
}