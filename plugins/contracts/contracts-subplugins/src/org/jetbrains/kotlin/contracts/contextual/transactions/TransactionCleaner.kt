/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions

import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.contracts.contextual.model.ContextCleaner
import org.jetbrains.kotlin.contracts.contextual.util.ThisInstanceHolder

internal class TransactionCleaner(private val openedTransaction: ThisInstanceHolder) : ContextCleaner {
    override val family = TransactionFamily

    override fun cleanupProcessed(context: Context): Context {
        if (context !is TransactionContext) throw AssertionError()

        val openedTransactions = context.openedTransactions.toMutableMap()
        openedTransactions.remove(openedTransaction)
        return TransactionContext(openedTransactions)
    }
}