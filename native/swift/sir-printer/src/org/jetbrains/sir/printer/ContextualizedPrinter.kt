/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.printer

import org.jetbrains.kotlin.utils.IndentingPrinter
import org.jetbrains.kotlin.utils.SmartPrinter

internal interface ContextualizedPrinter<Context> : IndentingPrinter {
    val currentContext: Context

    fun pushContext(context: Context)

    fun popContext()
}

internal fun <Context> ContextualizedPrinter<Context>.pushContext(transform: (Context) -> Context) {
    pushContext(transform(this.currentContext))
}

internal fun <T, R> ContextualizedPrinter<T>.withContext(context: T, action: () -> R): R =
    withContext(transform = { context }, action = action)

internal fun <T, R> ContextualizedPrinter<T>.withContext(transform: (T) -> T, action: () -> R): R {
    try {
        pushContext(transform)
        return action()
    } finally {
        popContext()
    }
}

internal open class ContextualizedPrinterImpl<T>(
    private val printer: SmartPrinter,
    context: T
) : ContextualizedPrinter<T>, IndentingPrinter by printer {
    private val contexts: MutableList<T> = mutableListOf(context)

    override val currentContext: T get() = contexts.last()

    override fun pushContext(context: T) {
        contexts.add(context)
    }

    override fun popContext() {
        contexts.removeLast()
    }
}