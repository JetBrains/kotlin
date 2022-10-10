/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions

class KotlinWithJavaCompilationFactory<KotlinOptionsType : KotlinCommonOptions, CO : KotlinCommonCompilerOptions>(
    override val target: KotlinWithJavaTarget<KotlinOptionsType, CO>,
    val compilerOptionsFactory: () -> HasCompilerOptions<CO>,
    val kotlinOptionsFactory: (CO) -> KotlinOptionsType
) : KotlinCompilationFactory<KotlinWithJavaCompilation<KotlinOptionsType, CO>> {

    override val itemClass: Class<KotlinWithJavaCompilation<KotlinOptionsType, CO>>
        @Suppress("UNCHECKED_CAST")
        get() = KotlinWithJavaCompilation::class.java as Class<KotlinWithJavaCompilation<KotlinOptionsType, CO>>

    @Suppress("UNCHECKED_CAST")
    override fun create(name: String): KotlinWithJavaCompilation<KotlinOptionsType, CO> {
        val compilerOptions = compilerOptionsFactory()
        val kotlinOptions = kotlinOptionsFactory(compilerOptions.options)
        return project.objects.newInstance(
            KotlinWithJavaCompilation::class.java,
            target,
            name,
            getOrCreateDefaultSourceSet(name),
            compilerOptions,
            kotlinOptions
        ) as KotlinWithJavaCompilation<KotlinOptionsType, CO>
    }
}