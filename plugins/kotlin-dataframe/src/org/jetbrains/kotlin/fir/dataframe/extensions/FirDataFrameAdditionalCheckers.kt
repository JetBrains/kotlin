/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.dataframe.InterpretationErrorReporter
import org.jetbrains.kotlin.fir.dataframe.pluginDataFrameSchema
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacadeImpl
import org.jetbrains.kotlinx.dataframe.plugin.ColumnPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.ColumnWithPathApproximation
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn

class FirDataFrameAdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(Checker())
    }
}

class Checker : FirFunctionCallChecker() {
    companion object {
        val ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)
        val CAST_ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.CALL_ELEMENT_WITH_DOT)
        val CAST_ID = CallableId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "api")), Name.identifier("cast"))
    }

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        with(KotlinTypeFacadeImpl(context.session)) {
            analyzeCast(expression, reporter, context)
            analyzeRefinedCallShape(expression, reporter = object : InterpretationErrorReporter {
                override var errorReported: Boolean = false

                override fun reportInterpretationError(call: FirFunctionCall, message: String) {
                    reporter.reportOn(call.source, ERROR, message, context)
                    errorReported = true
                }

                override fun doNotReportInterpretationError() {
                    errorReported = true
                }
            })
        }
    }

    private fun KotlinTypeFacadeImpl.analyzeCast(expression: FirFunctionCall, reporter: DiagnosticReporter, context: CheckerContext) {
        val calleeReference = expression.calleeReference
        if (calleeReference !is FirResolvedNamedReference || calleeReference.toResolvedCallableSymbol()?.callableId != CAST_ID) {
            return
        }
        val typeRef = expression.explicitReceiver?.typeRef
        if (typeRef != null) {

            val sourceType = typeRef.coneType.fullyExpandedType(session).typeArguments[0].type as? ConeClassLikeType
                ?: return
            val source = pluginDataFrameSchema(sourceType)
            val targetProjection = expression.typeArguments[0] as? FirTypeProjectionWithVariance ?: return
            val targetType = targetProjection.typeRef.coneType as? ConeClassLikeType ?: return
            val target = pluginDataFrameSchema(targetType)
            val sourceColumns = flatten(source)
            val targetColumns = flatten(target)
            val sourceMap = sourceColumns.associate { it.path.path to it.column }
            val missingColumns = mutableListOf<String>()
            var valid = true
            for (target in targetColumns) {
                val source = sourceMap[target.path.path]
                val present = if (source != null) {
                    if (source.type.type().isSubtypeOf(target.column.type.type(), session)) {
                        true
                    } else {
                        missingColumns += "${target.path.path} ${target.column.name}: ${source.type.type().renderReadable()} is not subtype of ${target.column.type.type()}"
                        false
                    }
                } else {
                    missingColumns += "${target.path.path} ${target.column.name} is missing"
                    false
                }

                valid = valid && present
            }
            if (!valid) {
                reporter.reportOn(expression.source, CAST_ERROR, "Cast cannot succeed \n ${missingColumns.joinToString("\n")}", context)
            }
        }
    }


    private fun flatten(schema: PluginDataFrameSchema): List<ColumnWithPathApproximation> {
        if (schema.columns().isEmpty()) return emptyList()
        val columns = mutableListOf<ColumnWithPathApproximation>()
        flattenImpl(schema.columns(), emptyList(), columns)
        return columns
    }

    private fun flattenImpl(columns: List<SimpleCol>, path: List<String>, flatList: MutableList<ColumnWithPathApproximation>) {
        columns.forEach { column ->
            val fullPath = path + listOf(column.name)
            when (column) {
                is SimpleColumnGroup -> {
                    flatList.add(ColumnWithPathApproximation(ColumnPathApproximation(fullPath), column))
                    flattenImpl(column.columns(), fullPath, flatList)
                }
                is SimpleFrameColumn -> {
                    flatList.add(ColumnWithPathApproximation(ColumnPathApproximation(fullPath), column))
                    flattenImpl(column.columns(), fullPath, flatList)
                }
                else -> {
                    flatList.add(ColumnWithPathApproximation(ColumnPathApproximation(fullPath), column))
                }
            }
        }
    }
}
