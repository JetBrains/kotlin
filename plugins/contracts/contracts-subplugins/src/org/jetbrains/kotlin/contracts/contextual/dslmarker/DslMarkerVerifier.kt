/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.dslmarker

import org.jetbrains.kotlin.contracts.contextual.cfg.ContextContracts
import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.contracts.contextual.model.ContextVerifier
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

internal class DslMarkerVerifier(val receiver: ReceiverValue, val sourceElement: KtElement) : ContextVerifier {
    override val family = DslMarkerFamily

    override fun verify(contexts: List<Context>, diagnosticSink: DiagnosticSink, declaredContracts: ContextContracts) {
        val lastContext = contexts.last() as? DslMarkerContext ?: throw AssertionError()

        if (receiver !in lastContext.receivers) {
            val message = "Call function in wrong scope"
            diagnosticSink.report(Errors.CONTEXTUAL_EFFECT_WARNING.on(sourceElement, message))
        }
    }
}