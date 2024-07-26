/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.reporting

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.formver.ErrorStyle
import org.jetbrains.kotlin.formver.PluginErrors
import org.jetbrains.kotlin.formver.embeddings.SourceRole
import org.jetbrains.kotlin.formver.viper.errors.ConsistencyError
import org.jetbrains.kotlin.formver.viper.errors.VerificationError
import org.jetbrains.kotlin.formver.viper.errors.VerifierError
import org.jetbrains.kotlin.formver.viper.errors.getInfoOrNull

class VerifierErrorInterpreter {
    private var isOriginalViperErrorReported = false

    private fun DiagnosticReporter.reportVerificationErrorUserFriendly(
        source: KtSourceElement?,
        error: VerificationError,
        context: CheckerContext,
    ) {
        when (val role = error.getInfoOrNull<SourceRole>()) {
            is SourceRole.ReturnsEffect ->
                reportOn(source, PluginErrors.UNEXPECTED_RETURNED_VALUE, role.asUserFriendlyMessage, context)
            is SourceRole.CallsInPlaceEffect ->
                reportOn(source, PluginErrors.INVALID_INVOCATION_TYPE, role.paramSymbol, role.kind.asUserFriendlyMessage, context)
            is SourceRole.ParamFunctionLeakageCheck -> with(role) {
                reportOn(source, PluginErrors.LAMBDA_MAY_LEAK, error.reason.fetchLeakingFunction(), context)
            }
            else -> reportVerificationErrorOriginalViper(source, error, context)
        }
    }

    private fun DiagnosticReporter.reportVerificationErrorOriginalViper(
        source: KtSourceElement?,
        error: VerificationError,
        context: CheckerContext,
    ) {
        if (!isOriginalViperErrorReported) {
            reportOn(source, PluginErrors.VIPER_VERIFICATION_ERROR, error.msg, context)
            isOriginalViperErrorReported = true
        }
    }

    private fun DiagnosticReporter.reportVerificationError(
        source: KtSourceElement?,
        error: VerificationError,
        errorStyle: ErrorStyle,
        context: CheckerContext,
    ) = when (errorStyle) {
        ErrorStyle.USER_FRIENDLY -> reportVerificationErrorUserFriendly(source, error, context)
        ErrorStyle.ORIGINAL_VIPER -> reportVerificationErrorOriginalViper(source, error, context)
        ErrorStyle.BOTH -> {
            reportVerificationErrorUserFriendly(source, error, context)
            reportVerificationErrorOriginalViper(source, error, context)
        }
    }

    private fun DiagnosticReporter.reportConsistencyError(source: KtSourceElement?, error: ConsistencyError, context: CheckerContext) {
        val sourceIsFunctionDeclaration = source?.elementType?.let { it == KtNodeTypes.FUN } ?: false
        val positionStrategy = when (sourceIsFunctionDeclaration) {
            true -> SourceElementPositioningStrategies.DECLARATION_NAME
            false -> SourceElementPositioningStrategies.DEFAULT
        }
        reportOn(source, PluginErrors.INTERNAL_ERROR, error.msg, context, positioningStrategy = positionStrategy)
    }

    fun DiagnosticReporter.reportVerifierError(
        source: KtSourceElement?,
        error: VerifierError,
        errorStyle: ErrorStyle,
        context: CheckerContext,
    ) = when (error) {
        is ConsistencyError -> reportConsistencyError(source, error, context)
        is VerificationError -> reportVerificationError(source, error, errorStyle, context)
    }

    private val EventOccurrencesRange.asUserFriendlyMessage: String
        get() = when (this) {
            EventOccurrencesRange.AT_MOST_ONCE -> "at most once"
            EventOccurrencesRange.EXACTLY_ONCE -> "exactly once"
            EventOccurrencesRange.AT_LEAST_ONCE -> "at least once"
            EventOccurrencesRange.MORE_THAN_ONCE -> "more than once"
            else -> TODO("Unreachable")
        }

    private val SourceRole.ReturnsEffect.asUserFriendlyMessage: String
        get() = when (this) {
            is SourceRole.ReturnsEffect.Bool -> if (bool) "false" else "true"
            is SourceRole.ReturnsEffect.Null -> if (negated) "null" else "non-null"
            else -> TODO("Unreachable")
        }
}