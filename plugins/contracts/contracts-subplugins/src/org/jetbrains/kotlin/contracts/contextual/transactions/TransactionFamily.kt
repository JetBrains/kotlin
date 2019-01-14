/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions

import org.jetbrains.kotlin.contracts.contextual.model.ContextFamily

internal object TransactionFamily : ContextFamily {
    override val id = "Transactions"
    override val combiner = TransactionCombiner
    override val emptyContext = TransactionContext()
}