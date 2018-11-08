/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.exceptions

import org.jetbrains.kotlin.contracts.contextual.cfg.ContextContracts
import org.jetbrains.kotlin.contracts.contextual.model.Context
import org.jetbrains.kotlin.contracts.contextual.model.ContextVerifier
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

internal class ExceptionVerifier(private val exceptionType: KotlinType, private val sourceElement: KtElement) : ContextVerifier {
    override val family = ExceptionFamily

    override fun verify(contexts: List<Context>, diagnosticSink: DiagnosticSink, declaredContracts: ContextContracts) {
        val exceptionContexts = contexts.map { it as? ExceptionContext ?: throw AssertionError() }

        val declaredExceptions = declaredContracts.verifiers
            .map { it as ExceptionVerifierDeclaration }
            .map { it.exceptionType }
            .toSet()

        val handledExceptions = exceptionContexts.flatMapTo(mutableSetOf()) { it.cachedExceptions }

        val handledAndDeclaredExceptions = handledExceptions + declaredExceptions
        val isOk = handledAndDeclaredExceptions.any { exceptionType.isSubtypeOf(it) }
        if (!isOk) {
            diagnosticSink.report(Errors.CONTEXTUAL_EFFECT_WARNING.on(sourceElement, "Unchecked exception: $exceptionType"))
        }
    }
}