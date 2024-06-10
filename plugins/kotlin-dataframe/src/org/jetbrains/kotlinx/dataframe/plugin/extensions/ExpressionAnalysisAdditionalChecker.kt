/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.flatten
import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.PluginDataFrameSchema

class ExpressionAnalysisAdditionalChecker(
    session: FirSession,
    cache: FirCache<String, PluginDataFrameSchema, KotlinTypeFacade>,
    schemasDirectory: String?
) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(Checker(cache, schemasDirectory))
    }
}

private class Checker(val cache: FirCache<String, PluginDataFrameSchema, KotlinTypeFacade>, val schemasDirectory: String?) : FirFunctionCallChecker(mppKind = MppCheckerKind.Common) {
    companion object {
        val ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)
        val CAST_ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.CALL_ELEMENT_WITH_DOT)
        val CAST_ID = CallableId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "api")), Name.identifier("cast"))
        val CHECK = ClassId(FqName("org.jetbrains.kotlinx.dataframe.annotations"), Name.identifier("Check"))
    }

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        with(KotlinTypeFacadeImpl(context.session, cache, schemasDirectory)) {
            analyzeCast(expression, reporter, context)
//            analyzeRefinedCallShape(expression, reporter = object : InterpretationErrorReporter {
//                override var errorReported: Boolean = false
//
//                override fun reportInterpretationError(call: FirFunctionCall, message: String) {
//                    reporter.reportOn(call.source, ERROR, message, context)
//                    errorReported = true
//                }
//
//                override fun doNotReportInterpretationError() {
//                    errorReported = true
//                }
//            })
        }
    }

    private fun KotlinTypeFacadeImpl.analyzeCast(expression: FirFunctionCall, reporter: DiagnosticReporter, context: CheckerContext) {
        val calleeReference = expression.calleeReference
        if (calleeReference !is FirResolvedNamedReference
            || calleeReference.toResolvedCallableSymbol()?.callableId != CAST_ID
            || !calleeReference.resolvedSymbol.hasAnnotation(CHECK, session)) {
            return
        }
        val coneType = expression.explicitReceiver?.resolvedType
        if (coneType != null) {
            val sourceType = coneType.fullyExpandedType(session).typeArguments.getOrNull(0)?.type as? ConeClassLikeType
                ?: return
            val source = pluginDataFrameSchema(sourceType)
            val targetProjection = expression.typeArguments.getOrNull(0) as? FirTypeProjectionWithVariance ?: return
            val targetType = targetProjection.typeRef.coneType as? ConeClassLikeType ?: return
            val target = pluginDataFrameSchema(targetType)
            val sourceColumns = source.flatten()
            val targetColumns = target.flatten()
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
}
