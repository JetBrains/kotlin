/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions

class KotlinWithJavaCompilationFactory<KotlinOptionsType : KotlinCommonOptions>(
    val project: Project,
    val target: KotlinWithJavaTarget<KotlinOptionsType>
) : KotlinCompilationFactory<KotlinWithJavaCompilation<KotlinOptionsType>> {

    override val itemClass: Class<KotlinWithJavaCompilation<KotlinOptionsType>>
        @Suppress("UNCHECKED_CAST")
        get() = KotlinWithJavaCompilation::class.java as Class<KotlinWithJavaCompilation<KotlinOptionsType>>

    override fun create(name: String): KotlinWithJavaCompilation<KotlinOptionsType> {
        val result = KotlinWithJavaCompilation(target, name)
        return result
    }
}