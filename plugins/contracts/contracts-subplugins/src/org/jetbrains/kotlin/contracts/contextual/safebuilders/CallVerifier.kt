/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.safebuilders

import org.jetbrains.kotlin.contracts.contextual.cfg.ContextContracts
import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.contracts.contextual.model.ContextVerifier
import org.jetbrains.kotlin.contracts.contextual.util.FunctionAndThisInstanceHolder
import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtElement

internal class CallVerifier(
    private val instanceHolder: FunctionAndThisInstanceHolder,
    private val expectedKind: InvocationKind,
    val sourceElement: KtElement
) : ContextVerifier {
    override val family = CallFamily

    // TODO: UNKNOWN and ZERO reports to lambda, others to calls
    override fun verify(contexts: List<Context>, diagnosticSink: DiagnosticSink, declaredContracts: ContextContracts) {
        val context = contexts.first() as? CallContext ?: throw AssertionError()

        val actualKind = context.calls[instanceHolder]?.kind ?: InvocationKind.ZERO
        if (!isSatisfied(expectedKind, actualKind)) {
            val message = "$instanceHolder call mismatch: expected $expectedKind, actual $actualKind"
            diagnosticSink.report(Errors.CONTEXTUAL_EFFECT_WARNING.on(sourceElement, message))
        }
    }

    private fun isSatisfied(expected: InvocationKind, actual: InvocationKind): Boolean {
        if (expected == InvocationKind.ZERO || expected == InvocationKind.UNKNOWN) throw AssertionError()
        if (actual == InvocationKind.UNKNOWN) return false

        if (actual == expected) return true
        if (expected == InvocationKind.AT_MOST_ONCE && (actual == InvocationKind.ZERO || actual == InvocationKind.EXACTLY_ONCE)) return true
        if (expected == InvocationKind.AT_LEAST_ONCE && actual == InvocationKind.EXACTLY_ONCE) return true

        return false
    }
}