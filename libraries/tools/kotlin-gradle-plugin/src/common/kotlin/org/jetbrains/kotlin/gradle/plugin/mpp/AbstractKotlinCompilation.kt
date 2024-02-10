/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl

@Deprecated("Use KotlinCompilation<T> instead")
abstract class AbstractKotlinCompilation<T : KotlinCommonOptions> internal constructor(compilation: KotlinCompilationImpl) :
    DecoratedKotlinCompilation<T>(compilation)

@Suppress("DEPRECATION")
internal typealias DeprecatedAbstractKotlinCompilation<T> = AbstractKotlinCompilation<T>
