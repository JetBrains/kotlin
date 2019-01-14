/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions

import org.jetbrains.kotlin.contracts.contextual.common.CallInfo
import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.contracts.contextual.model.ContextCombiner
import org.jetbrains.kotlin.contracts.contextual.model.ContextProvider
import org.jetbrains.kotlin.contracts.contextual.util.ThisInstanceHolder
import org.jetbrains.kotlin.contracts.description.InvocationKind

internal object TransactionCombiner : ContextCombiner {
    override fun or(a: Context, b: Context): Context {
        if (a !is TransactionContext || b !is TransactionContext) throw AssertionError()

        val openedTransactions = a.openedTransactions.keys union b.openedTransactions.keys

        val result = mutableMapOf<ThisInstanceHolder, CallInfo>()
        for (transaction in openedTransactions) {
            val aInfo = a.openedTransactions[transaction]
            val bInfo = b.openedTransactions[transaction]

            val aKind = aInfo?.kind ?: InvocationKind.ZERO
            val bKind = bInfo?.kind ?: InvocationKind.ZERO
            val resKind = InvocationKind.or(aKind, bKind)

            val sourceElement = aInfo?.sourceElement ?: bInfo?.sourceElement ?: throw AssertionError()
            result[transaction] = CallInfo(sourceElement, resKind)
        }

        return TransactionContext(result)
    }

    override fun combine(context: Context, provider: ContextProvider): Context {
        if (context !is TransactionContext || provider !is TransactionProvider) throw AssertionError()

        val (openedTransaction, sourceElement) = provider

        val openedTransactions = context.openedTransactions.toMutableMap()
        val currentKind = openedTransactions[openedTransaction]?.kind ?: InvocationKind.ZERO
        val resKind = InvocationKind.and(currentKind, InvocationKind.EXACTLY_ONCE)
        openedTransactions[openedTransaction] = CallInfo(sourceElement, resKind)
        return TransactionContext(openedTransactions)
    }
}