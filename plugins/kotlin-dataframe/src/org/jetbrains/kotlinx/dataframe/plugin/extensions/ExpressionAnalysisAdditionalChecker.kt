/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.AbstractSourceElementPositioningStrategy
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1DelegateProvider
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.flatten
import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import org.jetbrains.kotlinx.dataframe.plugin.utils.isDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.utils.isDataRow
import org.jetbrains.kotlinx.dataframe.plugin.utils.isGroupBy

class ExpressionAnalysisAdditionalChecker(
    session: FirSession,
    cache: FirCache<String, PluginDataFrameSchema, KotlinTypeFacade>,
    schemasDirectory: String?,
    isTest: Boolean,
    dumpSchemas: Boolean
) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOfNotNull(
            Checker(cache, schemasDirectory, isTest), FunctionCallSchemaReporter.takeIf { dumpSchemas }
        )
        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = setOfNotNull(
            PropertyAccessSchemaReporter.takeIf { dumpSchemas }
        )
    }
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> = setOfNotNull(PropertySchemaReporter.takeIf { dumpSchemas })
        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> = setOfNotNull(FunctionDeclarationSchemaReporter.takeIf { dumpSchemas })
    }
}

private class Checker(
    val cache: FirCache<String, PluginDataFrameSchema, KotlinTypeFacade>,
    val schemasDirectory: String?,
    val isTest: Boolean,
) : FirFunctionCallChecker(mppKind = MppCheckerKind.Common) {
    companion object {
        val ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)
        val CAST_ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.CALL_ELEMENT_WITH_DOT)
        val CAST_TARGET_WARNING by warning1<KtElement, String>(SourceElementPositioningStrategies.CALL_ELEMENT_WITH_DOT)
        val CAST_ID = CallableId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "api")), Name.identifier("cast"))
        val CHECK = ClassId(FqName("org.jetbrains.kotlinx.dataframe.annotations"), Name.identifier("Check"))
    }

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        with(KotlinTypeFacadeImpl(context.session, cache, schemasDirectory, isTest)) {
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
        val targetProjection = expression.typeArguments.getOrNull(0) as? FirTypeProjectionWithVariance ?: return
        val targetType = targetProjection.typeRef.coneType as? ConeClassLikeType ?: return
        val targetSymbol = targetType.toSymbol(session)
        if (targetSymbol != null && !targetSymbol.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, session)) {
            val text = "Annotate ${targetType.renderReadable()} with @DataSchema to use generated properties"
            reporter.reportOn(expression.source, CAST_TARGET_WARNING, text, context)
        }
        val coneType = expression.explicitReceiver?.resolvedType
        if (coneType != null) {
            val sourceType = coneType.fullyExpandedType(session).typeArguments.getOrNull(0)?.type as? ConeClassLikeType
                ?: return
            val source = pluginDataFrameSchema(sourceType)
            val target = pluginDataFrameSchema(targetType)
            val sourceColumns = source.flatten(includeFrames = true)
            val targetColumns = target.flatten(includeFrames = true)
            val sourceMap = sourceColumns.associate { it.path.path to it.column }
            val missingColumns = mutableListOf<String>()
            var valid = true
            for (target in targetColumns) {
                val source = sourceMap[target.path.path]
                val present = if (source != null) {
                    if (source !is SimpleDataColumn || target.column !is SimpleDataColumn) { continue }
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

private data object PropertySchemaReporter : FirPropertyChecker(mppKind = MppCheckerKind.Common) {
    val SCHEMA by info1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        context.sessionContext {
            declaration.returnTypeRef.coneType.let { type ->
                reportSchema(reporter, declaration.source, SCHEMA, type, context)
            }
        }
    }
}

private data object FunctionCallSchemaReporter : FirFunctionCallChecker(mppKind = MppCheckerKind.Common) {
    val SCHEMA by info1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.calleeReference.name in setOf(Name.identifier("let"), Name.identifier("run"))) return
        val initializer = expression.resolvedType
        context.sessionContext {
            reportSchema(reporter, expression.source, SCHEMA, initializer, context)
        }
    }
}

private data object PropertyAccessSchemaReporter : FirPropertyAccessExpressionChecker(mppKind = MppCheckerKind.Common) {
    val SCHEMA by info1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)

    override fun check(
        expression: FirPropertyAccessExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val initializer = expression.resolvedType
        context.sessionContext {
            reportSchema(reporter, expression.source, SCHEMA, initializer, context)
        }
    }
}

private data object FunctionDeclarationSchemaReporter : FirSimpleFunctionChecker(mppKind = MppCheckerKind.Common) {
    val SCHEMA by info1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)

    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val type = declaration.returnTypeRef.coneType
        context.sessionContext {
            reportSchema(reporter, declaration.source, SCHEMA, type, context)
        }
    }
}

private fun SessionContext.reportSchema(
    reporter: DiagnosticReporter,
    source: KtSourceElement?,
    factory: KtDiagnosticFactory1<String>,
    type: ConeKotlinType,
    context: CheckerContext,
) {
    val expandedType = type.fullyExpandedType(session)
    var schema: PluginDataFrameSchema? = null
    when {
        expandedType.isDataFrame(session) -> {
            schema = expandedType.typeArguments.getOrNull(0)?.let {
                pluginDataFrameSchema(it)
            }
        }

        expandedType.isDataRow(session) -> {
            schema = expandedType.typeArguments.getOrNull(0)?.let {
                pluginDataFrameSchema(it)
            }
        }

        expandedType.isGroupBy(session) -> {
            val keys = expandedType.typeArguments.getOrNull(0)
            val grouped = expandedType.typeArguments.getOrNull(1)
            if (keys != null && grouped != null) {
                val keysSchema = pluginDataFrameSchema(keys)
                val groupedSchema = pluginDataFrameSchema(grouped)
                schema = PluginDataFrameSchema(
                    listOf(
                        SimpleColumnGroup("keys", keysSchema.columns()),
                        SimpleFrameColumn("groups", groupedSchema.columns())
                    )
                )
            }
        }
    }
    if (schema != null && source != null) {
        reporter.reportOn(source, factory, "\n" + schema.toString(), context)
    }
}

fun CheckerContext.sessionContext(f: SessionContext.() -> Unit) {
    SessionContext(session).f()
}

inline fun <reified P : PsiElement, A> info1(
    positioningStrategy: AbstractSourceElementPositioningStrategy = SourceElementPositioningStrategies.DEFAULT
): DiagnosticFactory1DelegateProvider<A> {
    return DiagnosticFactory1DelegateProvider(Severity.INFO, positioningStrategy, P::class)
}
