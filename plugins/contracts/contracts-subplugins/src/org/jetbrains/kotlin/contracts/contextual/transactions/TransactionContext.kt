/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions

import org.jetbrains.kotlin.contracts.contextual.cfg.ContextContracts
import org.jetbrains.kotlin.contracts.contextual.common.CallInfo
import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.contracts.contextual.util.ThisInstanceHolder
import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors

internal data class TransactionContext(val openedTransactions: Map<ThisInstanceHolder, CallInfo> = mapOf()) : Context {
    override val family = TransactionFamily

    override fun reportRemaining(sink: DiagnosticSink, declaredContracts: ContextContracts) {
        for ((instanceHolder, info) in openedTransactions) {
            if (info.kind != InvocationKind.ZERO) {
                val message = "Transaction $instanceHolder must be closed"
                sink.report(Errors.CONTEXTUAL_EFFECT_WARNING.on(info.sourceElement, message))
            }
        }
    }
}