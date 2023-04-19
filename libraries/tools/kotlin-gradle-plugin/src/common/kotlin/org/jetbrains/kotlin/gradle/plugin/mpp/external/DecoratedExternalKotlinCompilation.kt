/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.DecoratedKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl

/**
 * Renamed DecoratedExternalKotlinCompilation:
 * Scheduled for removal with Kotlin 2.0
 */
@Deprecated(
    "Renamed to 'DecoratedExternalKotlinCompilation'", level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("DecoratedExternalKotlinCompilation")
)
@ExternalKotlinTargetApi
abstract class ExternalDecoratedKotlinCompilation(delegate: Delegate) :
    DecoratedKotlinCompilation<KotlinCommonOptions>(delegate.compilation) {
    open class Delegate internal constructor(internal open val compilation: KotlinCompilationImpl)
}

@Suppress("deprecation_error")
@ExternalKotlinTargetApi
abstract class DecoratedExternalKotlinCompilation(delegate: Delegate) : ExternalDecoratedKotlinCompilation(delegate) {
    class Delegate internal constructor(compilation: KotlinCompilationImpl) : ExternalDecoratedKotlinCompilation.Delegate(compilation)
}