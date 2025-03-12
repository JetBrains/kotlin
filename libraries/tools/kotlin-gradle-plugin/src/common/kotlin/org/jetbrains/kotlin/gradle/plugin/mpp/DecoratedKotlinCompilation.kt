/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinAnyOptionsDeprecated
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl

@InternalKotlinGradlePluginApi
abstract class DecoratedKotlinCompilation<T : KotlinAnyOptionsDeprecated> internal constructor(
    internal val compilation: KotlinCompilationImpl,
) : InternalKotlinCompilation<T> by compilation as InternalKotlinCompilation<T> {
    override fun toString(): String = compilation.toString()
}

internal inline val <T : KotlinAnyOptionsDeprecated> InternalKotlinCompilation<T>.decoratedInstance: DecoratedKotlinCompilation<T>
    get() = this as? DecoratedKotlinCompilation<T>
        ?: target.compilations.getByName(compilationName).internal as DecoratedKotlinCompilation<T>
