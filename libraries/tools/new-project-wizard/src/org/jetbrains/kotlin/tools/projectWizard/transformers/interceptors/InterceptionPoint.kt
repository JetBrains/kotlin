/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors

data class InterceptionPoint<out T: Any>(val name: String, val initialValue: T)

data class InterceptionPointModifier<out T : Any>(
    val point: InterceptionPoint<@UnsafeVariance T>,
    val modifier: (@UnsafeVariance T) -> T
)