/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.reporting

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.formver.PluginErrors
import org.jetbrains.kotlin.formver.embeddings.SourceRole
import org.jetbrains.kotlin.formver.viper.ast.info
import org.jetbrains.kotlin.formver.viper.ast.unwrap
import org.jetbrains.kotlin.formver.viper.errors.VerificationError

sealed interface FormattedError {
    fun report(reporter: DiagnosticReporter, source: KtSourceElement?, context: CheckerContext)
}

class ReturnsEffectError(private val sourceRole: SourceRole.ReturnsEffect) : FormattedError {
    private val SourceRole.ReturnsEffect.asUserFriendlyMessage: String
        get() = when (this) {
            is SourceRole.ReturnsEffect.Bool -> if (bool) "false" else "true"
            is SourceRole.ReturnsEffect.Null -> if (negated) "null" else "non-null"
            else -> throw IllegalStateException("Unknown returns effect: $this")
        }

    override fun report(reporter: DiagnosticReporter, source: KtSourceElement?, context: CheckerContext) {
        reporter.reportOn(source, PluginErrors.UNEXPECTED_RETURNED_VALUE, sourceRole.asUserFriendlyMessage, context)
    }
}

class CallsInPlaceError(private val sourceRole: SourceRole.CallsInPlaceEffect) : FormattedError {
    private val SourceRole.CallsInPlaceEffect.asUserFriendlyMessage: String
        get() = when (kind) {
            EventOccurrencesRange.AT_MOST_ONCE -> "at most once"
            EventOccurrencesRange.EXACTLY_ONCE -> "exactly once"
            EventOccurrencesRange.AT_LEAST_ONCE -> "at least once"
            EventOccurrencesRange.MORE_THAN_ONCE -> "more than once"
            else -> throw IllegalStateException("Unknown kind of range: $kind")
        }

    override fun report(reporter: DiagnosticReporter, source: KtSourceElement?, context: CheckerContext) {
        reporter.reportOn(source, PluginErrors.INVALID_INVOCATION_TYPE, sourceRole.paramSymbol, sourceRole.asUserFriendlyMessage, context)
    }
}

class LeakingLambdaError(private val error: VerificationError) : FormattedError {
    override fun report(reporter: DiagnosticReporter, source: KtSourceElement?, context: CheckerContext) {
        // The leaking function symbol is always contained in the first argument of the error's reason.
        val faultPropositionInfo = error.unverifiableProposition.asCallable().arg(0).info
        val leakingFunctionSymbol = faultPropositionInfo.unwrap<SourceRole.FirSymbolHolder>().firSymbol
        reporter.reportOn(source, PluginErrors.LAMBDA_MAY_LEAK, leakingFunctionSymbol, context)
    }
}

class ConditionalEffectError(private val sourceRole: SourceRole.ConditionalEffect) : FormattedError {
    private val SourceRole.ReturnsEffect.asUserFriendlyMessage: String
        get() = when (this) {
            is SourceRole.ReturnsEffect.Bool, is SourceRole.ReturnsEffect.Null -> "a $this value is returned"
            SourceRole.ReturnsEffect.Wildcard -> "the function returns"
        }

    override fun report(reporter: DiagnosticReporter, source: KtSourceElement?, context: CheckerContext) {
        val (returnEffect, condition) = sourceRole
        val returnEffectMsg = returnEffect.asUserFriendlyMessage
        val conditionPrettyPrinted = with(SourceRoleConditionPrettyPrinter) {
            condition.prettyPrint()
        }
        reporter.reportOn(source, PluginErrors.CONDITIONAL_EFFECT_ERROR, returnEffectMsg, conditionPrettyPrinted, context)
    }
}

class DefaultError(private val error: VerificationError) : FormattedError {
    override fun report(reporter: DiagnosticReporter, source: KtSourceElement?, context: CheckerContext) {
        reporter.reportOn(source, PluginErrors.VIPER_VERIFICATION_ERROR, error.msg, context)
    }
}

fun VerificationError.formatUserFriendly(): FormattedError? =
    when (val sourceRole = lookupSourceRole()) {
        is SourceRole.ReturnsEffect -> ReturnsEffectError(sourceRole)
        is SourceRole.CallsInPlaceEffect -> CallsInPlaceError(sourceRole)
        is SourceRole.ParamFunctionLeakageCheck -> LeakingLambdaError(this)
        is SourceRole.ConditionalEffect -> ConditionalEffectError(sourceRole)
        else -> null
    }

/**
 * Find the contained [SourceRole] within a verification error.
 * If the role is not found, then returns `null`.
 */
private fun VerificationError.lookupSourceRole(): SourceRole? {
    /**
     * Lookup strategy:
     * The source role can be embedded either in the error's location node, or in the fault proposition.
     *
     * As an example, `PreconditionInCallFalse` errors have as offending node result the call-site of the called method.
     * But the actual info we are interested in is on the pre-condition, contained in the reason's offending node.
     */
    return when (val locationNodeRole = locationNode.getInfoOrNull<SourceRole>()) {
        null -> unverifiableProposition.getInfoOrNull<SourceRole>()
        else -> locationNodeRole
    }
}