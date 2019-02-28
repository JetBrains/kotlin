/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

class KotlinNativeCompilationFactory(
    val project: Project,
    val target: KotlinNativeTarget
) : KotlinCompilationFactory<KotlinNativeCompilation> {

    override val itemClass: Class<KotlinNativeCompilation>
        get() = KotlinNativeCompilation::class.java

    override fun create(name: String): KotlinNativeCompilation =
        KotlinNativeCompilation(target, name).apply {
            if (name == KotlinCompilation.TEST_COMPILATION_NAME) {
                friendCompilationName = KotlinCompilation.MAIN_COMPILATION_NAME
                isTestCompilation = true
            }
            buildTypesNoWarn = mutableListOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)
        }

}