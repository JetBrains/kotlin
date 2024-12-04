/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * A checker which reports redundant/inefficient creation of `Json` objects.
 *
 * For K1 alternative of this checker, see [org.jetbrains.kotlin.idea.compilerPlugin.kotlinxSerialization.JsonFormatRedundantDiagnostic]
 * in the IntelliJ IDEA repository.
 */
internal object FirSerializationPluginCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val jsonCallableId: CallableId = CallableId(FqName("kotlinx.serialization.json"), callableName = Name.identifier("Json"))
    private val jsonDefaultClassId: ClassId = ClassId.fromString("kotlinx/serialization/json/Json.Default")
    private val parameterNameFrom: Name = Name.identifier("from")
    private val parameterNameBuilderAction: Name = Name.identifier("builderAction")

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val function = expression.calleeReference.symbol as? FirNamedFunctionSymbol ?: return
        if (!isJsonFormatCreation(function)) return

        if (isDefaultFormat(expression)) {
            reporter.reportOn(
                expression.source,
                FirSerializationErrors.JSON_FORMAT_REDUNDANT_DEFAULT,
                context
            )
        } else {
            val parentCall = context.callsOrAssignments.getOrNull(context.callsOrAssignments.size - 2) as? FirFunctionCall ?: return
            if (parentCall.explicitReceiver !== expression) return
            reporter.reportOn(
                expression.source,
                FirSerializationErrors.JSON_FORMAT_REDUNDANT,
                context
            )
        }
    }

    private fun isJsonFormatCreation(function: FirNamedFunctionSymbol): Boolean {
        return function.callableId == jsonCallableId
    }

    private fun isDefaultFormat(functionCall: FirFunctionCall): Boolean {
        var defaultFrom = true // if no argument is passed, the default value is Json.Default
        var emptyBuilder = false

        functionCall.resolvedArgumentMapping?.forEach { (argumentExpression, parameter) ->
            when (parameter.name) {
                parameterNameFrom -> {
                    defaultFrom = isDefaultFormatArgument(argumentExpression)
                }
                parameterNameBuilderAction -> {
                    emptyBuilder = isEmptyFunctionArgument(argumentExpression)
                }
            }
        }

        return defaultFrom && emptyBuilder
    }

    private fun isDefaultFormatArgument(argumentExpression: FirExpression): Boolean {
        val typeRef = argumentExpression.resolvedType as? ConeClassLikeType ?: return false
        return typeRef.lookupTag.classId == jsonDefaultClassId
    }

    private fun isEmptyFunctionArgument(argument: FirExpression): Boolean {
        val lambdaArgument = (argument as? FirAnonymousFunctionExpression)?.anonymousFunction?.body ?: return false

        return lambdaArgument.statements.isEmpty() ||
                lambdaArgument.statements.singleOrNull()?.source?.kind == KtFakeSourceElementKind.ImplicitReturn.FromExpressionBody
    }
}
