/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl

@Suppress("DEPRECATION")
@Deprecated("Use KotlinCompilation<T> instead. Scheduled for removal in Kotlin 2.3.", level = DeprecationLevel.ERROR)
abstract class AbstractKotlinCompilation<T : KotlinCommonOptions> internal constructor(compilation: KotlinCompilationImpl) :
    DecoratedKotlinCompilation<T>(compilation)

@Suppress("DEPRECATION_ERROR")
internal typealias DeprecatedAbstractKotlinCompilation<T> = AbstractKotlinCompilation<T>
