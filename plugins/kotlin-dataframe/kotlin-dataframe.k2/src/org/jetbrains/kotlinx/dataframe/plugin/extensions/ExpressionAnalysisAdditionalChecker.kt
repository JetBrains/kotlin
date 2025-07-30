/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
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
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.CAST_ERROR
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.CAST_TARGET_WARNING
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.ERROR
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
    isTest: Boolean,
    dumpSchemas: Boolean,
) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOfNotNull(
            Checker(isTest), FunctionCallSchemaReporter.takeIf { dumpSchemas }
        )
        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = setOfNotNull(
            PropertyAccessSchemaReporter.takeIf { dumpSchemas }
        )
    }
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> = setOfNotNull(PropertySchemaReporter.takeIf { dumpSchemas })
        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> =
            setOfNotNull(FunctionDeclarationSchemaReporter.takeIf { dumpSchemas })
    }
}

object FirDataFrameErrors : KtDiagnosticsContainer() {
    val ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)
    val CAST_ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.CALL_ELEMENT_WITH_DOT)
    val CAST_TARGET_WARNING by warning1<KtElement, String>(SourceElementPositioningStrategies.CALL_ELEMENT_WITH_DOT)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = DataFrameDiagnosticMessages
}

object DataFrameDiagnosticMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrameDiagnosticMessages") { map ->
        map.put(ERROR, "{0}", TO_STRING)
        map.put(CAST_ERROR, "{0}", TO_STRING)
        map.put(CAST_TARGET_WARNING, "{0}", TO_STRING)
    }
}

private class Checker(
    val isTest: Boolean,
) : FirFunctionCallChecker(mppKind = MppCheckerKind.Common) {
    companion object {
        val CAST_ID = CallableId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "api")), Name.identifier("cast"))
        val CHECK = ClassId(FqName("org.jetbrains.kotlinx.dataframe.annotations"), Name.identifier("Check"))
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        with(KotlinTypeFacadeImpl(context.session, isTest)) {
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
            || !calleeReference.resolvedSymbol.hasAnnotation(CHECK, session)
        ) {
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
                    if (source !is SimpleDataColumn || target.column !is SimpleDataColumn) {
                        continue
                    }
                    if (source.type.type().isSubtypeOf(target.column.type.type(), session)) {
                        true
                    } else {
                        missingColumns += "${target.path.path} ${target.column.name}: ${
                            source.type.type().renderReadable()
                        } is not subtype of ${target.column.type.type()}"
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
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        context.sessionContext {
            declaration.returnTypeRef.coneType.let { type ->
                reportSchema(reporter, declaration.source, SchemaInfoDiagnostics.PROPERTY_SCHEMA, type, context)
            }
        }
    }
}

private data object FunctionCallSchemaReporter : FirFunctionCallChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (expression.calleeReference.name in setOf(Name.identifier("let"), Name.identifier("run"))) return
        val initializer = expression.resolvedType
        context.sessionContext {
            reportSchema(reporter, expression.source, SchemaInfoDiagnostics.FUNCTION_CALL_SCHEMA, initializer, context)
        }
    }
}

private data object PropertyAccessSchemaReporter : FirPropertyAccessExpressionChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        val initializer = expression.resolvedType
        context.sessionContext {
            reportSchema(reporter, expression.source, SchemaInfoDiagnostics.PROPERTY_ACCESS_SCHEMA, initializer, context)
        }
    }
}

private data object FunctionDeclarationSchemaReporter : FirSimpleFunctionChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        val type = declaration.returnTypeRef.coneType
        context.sessionContext {
            reportSchema(reporter, declaration.source, SchemaInfoDiagnostics.FUNCTION_SCHEMA, type, context)
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

object SchemaInfoDiagnostics : KtDiagnosticsContainer() {
    val PROPERTY_SCHEMA by info1(SourceElementPositioningStrategies.DECLARATION_NAME)
    val FUNCTION_SCHEMA by info1(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val FUNCTION_CALL_SCHEMA by info1(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val PROPERTY_ACCESS_SCHEMA by info1(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = SchemaRenderers

    private object SchemaRenderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrame Schemas") {
            it.put(PROPERTY_SCHEMA, "{0}", TO_STRING)
            it.put(FUNCTION_SCHEMA, "{0}", TO_STRING)
            it.put(FUNCTION_CALL_SCHEMA, "{0}", TO_STRING)
            it.put(PROPERTY_ACCESS_SCHEMA, "{0}", TO_STRING)
        }
    }
}

context(container: KtDiagnosticsContainer)
internal fun info1(positioningStrategy: AbstractSourceElementPositioningStrategy): DiagnosticFactory1DelegateProvider<String> {
    return DiagnosticFactory1DelegateProvider(
        Severity.INFO,
        positioningStrategy,
        KtElement::class,
        container = container,
    )
}

fun CheckerContext.sessionContext(f: SessionContext.() -> Unit) {
    SessionContext(session).f()
}
