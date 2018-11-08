/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.model

interface ContextCombiner {
    fun or(a: Context, b: Context): Context
    fun combine(context: Context, provider: ContextProvider): Context
}