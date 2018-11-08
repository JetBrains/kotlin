/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.dslmarker

import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.contracts.contextual.model.ContextCombiner
import org.jetbrains.kotlin.contracts.contextual.model.ContextProvider

internal object DslMarkerCombiner : ContextCombiner {
    override fun or(a: Context, b: Context): Context {
        if (a !is DslMarkerContext || b !is DslMarkerContext) throw AssertionError()
        return DslMarkerContext(a.receivers + b.receivers)
    }

    override fun combine(context: Context, provider: ContextProvider): Context {
        if (context !is DslMarkerContext || provider !is DslMarkerProvider) throw AssertionError()
        return DslMarkerContext(context.receivers + provider.receiver)
    }
}