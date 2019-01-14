/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions

import org.jetbrains.kotlin.contracts.contextual.model.ContextProvider
import org.jetbrains.kotlin.contracts.contextual.util.ThisInstanceHolder
import org.jetbrains.kotlin.psi.KtElement

internal data class TransactionProvider(val openedTransaction: ThisInstanceHolder, val sourceElement: KtElement) : ContextProvider {
    override val family = TransactionFamily
}