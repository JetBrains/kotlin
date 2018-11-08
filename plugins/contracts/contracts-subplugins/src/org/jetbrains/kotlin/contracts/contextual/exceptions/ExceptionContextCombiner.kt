/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.exceptions

import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.contracts.contextual.model.ContextCombiner
import org.jetbrains.kotlin.contracts.contextual.model.ContextProvider

internal object ExceptionContextCombiner : ContextCombiner {
    override fun or(a: Context, b: Context): Context {
        if (a !is ExceptionContext || b !is ExceptionContext) throw AssertionError()
        return ExceptionContext(a.cachedExceptions + b.cachedExceptions)
    }

    override fun combine(context: Context, provider: ContextProvider): Context {
        if (context !is ExceptionContext || provider !is ExceptionContextProvider) throw AssertionError()
        return ExceptionContext(context.cachedExceptions + provider.cachedException)
    }
}