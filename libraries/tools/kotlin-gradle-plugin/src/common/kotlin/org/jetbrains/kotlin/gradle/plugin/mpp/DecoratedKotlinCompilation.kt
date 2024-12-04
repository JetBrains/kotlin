/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl

@Suppress("DEPRECATION")
@InternalKotlinGradlePluginApi
abstract class DecoratedKotlinCompilation<T : KotlinCommonOptions> internal constructor(
    internal val compilation: KotlinCompilationImpl,
) : InternalKotlinCompilation<T> by compilation as InternalKotlinCompilation<T> {
    override fun toString(): String = compilation.toString()
}

@Suppress("DEPRECATION")
internal inline val <reified T : KotlinCommonOptions> InternalKotlinCompilation<T>.decoratedInstance: DecoratedKotlinCompilation<T>
    get() = if (this is DecoratedKotlinCompilation<T>) this
    else (target.compilations.getByName(compilationName).internal.castKotlinOptionsType<T>() as DecoratedKotlinCompilation<T>)
