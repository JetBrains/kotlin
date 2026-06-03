/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableReference
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.impl.toCamelCaseByDelimiters
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.CAST_ERROR
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.CAST_TARGET_WARNING
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_EXTENSION_PROPERTY_SHADOWED
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_PLUGIN_IS_DISABLED
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_GENERIC
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATA_SCHEMA_DECLARATION_VISIBILITY
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATA_SCHEMA_LOCAL_DECLARATION
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.MATERIALIZED_SCHEMA_INFO
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.MATERIALIZED_SCHEMA_ON_CAST
import org.jetbrains.kotlinx.dataframe.plugin.extensions.impl.toMaterializedSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.flatten
import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.utils.ALLOWED_DECLARATION_VISIBILITY
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class ExpressionAnalysisAdditionalChecker(
    session: FirSession,
    isTest: Boolean,
    dumpSchemas: Boolean,
) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOfNotNull(
            Checker(isTest, dumpSchemas),
            DataFrameFunctionCallTransformationContextChecker,
        )
        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = setOf(ShadowedExtensionPropertyChecker)
    }
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(DataSchemaDeclarationChecker)
        override val propertyCheckers: Set<FirPropertyChecker> = setOf(DataFramePropertyChecker)
    }
}

private class Checker(
    val isTest: Boolean,
    val dumpSchemas: Boolean,
) : FirFunctionCallChecker(mppKind = MppCheckerKind.Common) {
    companion object {
        val CAST_ID = CallableId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "api")), Name.identifier("cast"))
        val CHECK = ClassId(FqName("org.jetbrains.kotlinx.dataframe.annotations"), Name.identifier("Check"))
        val VALID_CAST_TARGET_PREDICATE = LookupPredicate.create {
            annotated(
                Names.DATA_SCHEMA_CLASS_ID.asSingleFqName()
            ) or annotated(
                Names.DATA_SCHEMA_SOURCE_CLASS_ID.asSingleFqName()
            )
        }
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
        val targetType = expression.getCastTargetType(reporter, context) ?: return
        val source = expression.dataFrameReceiverSchema()?.schema ?: return
        if (source.columns().isEmpty()) return
        val target = targetType.pluginDataFrameSchema()
        validateSchemaCompatibility(source, target, reporter, expression, context)
        val asDataClass = targetType.toRegularClassSymbol()?.isInterface == false
        if (target.columns().isEmpty()) {
            expression.source?.let {
                reportMaterializedSchemaWarning(source, targetType, asDataClass, it, reporter, context)
            }
        }
        if (dumpSchemas) {
            reporter.reportOn(
                expression.typeArguments.getOrNull(0)?.source,
                MATERIALIZED_SCHEMA_INFO,
                source.toMaterializedSchema(targetType.renderReadable(), asDataClass),
                context
            )
        }
    }

    private fun KotlinTypeFacadeImpl.validateSchemaCompatibility(
        source: PluginDataFrameSchema,
        target: PluginDataFrameSchema,
        reporter: DiagnosticReporter,
        expression: FirFunctionCall,
        context: CheckerContext,
    ) {
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
                if (source.type.coneType.isSubtypeOf(target.column.type.coneType, session)) {
                    true
                } else {
                    missingColumns += "${target.path.path} ${target.column.name}: ${
                        source.type.coneType.renderReadable()
                    } is not subtype of ${target.column.type.coneType}"
                    false
                }
            } else {
                missingColumns += "${target.path.path} ${target.column.name} is missing"
                false
            }

            valid = valid && present
        }
        if (!valid) {
            reporter.reportOn(expression.source, CAST_ERROR, missingColumns.joinToString("\n"), context)
        }
    }

    context(sessionHolder: SessionHolder)
    private fun FirFunctionCall.getCastTargetType(
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ): ConeClassLikeType? {
        val targetProjection = typeArguments.getOrNull(0) as? FirTypeProjectionWithVariance ?: return null
        val targetType = targetProjection.typeRef.coneType as? ConeClassLikeType ?: return null
        val targetSymbol = targetType.toSymbol()
        if (targetSymbol != null && !sessionHolder.session.predicateBasedProvider.matches(VALID_CAST_TARGET_PREDICATE, targetSymbol)) {
            reporter.reportOn(source, CAST_TARGET_WARNING, targetType.renderReadable(), context)
        }
        return targetType
    }

    context(_: SessionHolder)
    private fun reportMaterializedSchemaWarning(
        source: PluginDataFrameSchema,
        targetType: ConeClassLikeType,
        asDataClass: Boolean,
        expression: KtSourceElement,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        reporter.reportOn(
            expression,
            MATERIALIZED_SCHEMA_ON_CAST,
            source.toMaterializedSchema(targetType.renderReadable(), asDataClass),
            context
        )
    }
}

context(sessionHolder: SessionHolder)
fun FirFunctionCall.dataFrameReceiverSchema(): ExpressionDataFrameSchema? {
    val resolvedMarker = explicitReceiver
        ?.resolvedType
        ?.fullyExpandedType()?.typeArguments?.getOrNull(0)?.type
        ?: return null

    return ExpressionDataFrameSchema(resolvedMarker, resolvedMarker.pluginDataFrameSchema())
}

data class ExpressionDataFrameSchema(val type: ConeKotlinType, val schema: PluginDataFrameSchema)

fun String.toDataSchemaName(): String = toCamelCaseByDelimiters().replaceFirstChar { it.uppercase() }

internal object DataSchemaDeclarationChecker : FirRegularClassChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val annotated = declaration.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, context.session) ||
                declaration.hasAnnotation(Names.DATA_SCHEMA_SOURCE_CLASS_ID, context.session)
        if (!annotated) return
        if (declaration.isLocal) {
            reporter.reportOn(
                declaration.source,
                DATA_SCHEMA_LOCAL_DECLARATION,
                context
            )
        } else if (declaration.effectiveVisibility !in ALLOWED_DECLARATION_VISIBILITY) {
            val visibilityOptions = ALLOWED_DECLARATION_VISIBILITY.joinToString(", ")
            reporter.reportOn(
                declaration.source,
                DATA_SCHEMA_DECLARATION_VISIBILITY,
                visibilityOptions
            )
        }
    }
}

private data object DataFrameFunctionCallTransformationContextChecker : FirFunctionCallChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        expression.toResolvedCallableReference()?.toResolvedNamedFunctionSymbol()?.let { symbol ->
            val shouldRefine = FunctionCallTransformer.shouldRefine(expression.annotations, symbol, context.session)
            if (!shouldRefine) return
            val disabled =
                context.containingDeclarations
                    .firstOrNull { it.hasAnnotation(Names.DISABLE_INTERPRETATION_ANNOTATION, context.session) }

            if (context.containingDeclarations.any { it is FirNamedFunctionSymbol && it.isInline }) {
                if (disabled == null) {
                    reporter.reportOn(
                        expression.source,
                        DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE
                    )
                }
            }

            if (context.containingDeclarations.any { it.typeParameterSymbols?.isNotEmpty() == true }) {
                if (disabled == null) {
                    reporter.reportOn(
                        expression.source,
                        DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_GENERIC
                    )
                }
            }

            if (context.containingDeclarations.lastOrNull() is FirPropertyAccessorSymbol) {
                if (disabled == null) {
                    reporter.reportOn(
                        expression.source,
                        DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR
                    )
                }
            }

            if (disabled != null) {
                reporter.reportOn(
                    expression.source,
                    DATAFRAME_PLUGIN_IS_DISABLED,
                    disabled.name.toString()
                )
            }
        }
    }
}

private val FirBasedSymbol<*>.name
    get() = when (this) {
        is FirClassLikeSymbol<*> -> name
        is FirCallableSymbol<*> -> name
        is FirFileSymbol -> sourceFile?.name ?: toString()
        else -> toString()
    }

private data object DataFramePropertyChecker : FirPropertyChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val typeArgument = declaration.symbol.resolvedReturnType.typeArguments.getOrNull(0) as? ConeClassLikeType ?: return
        val typeArgumentSymbol = typeArgument.toRegularClassSymbol() ?: return
        val origin = typeArgumentSymbol.origin
        val schema = typeArgument.pluginDataFrameSchema()
        if (context.findClosest<FirScriptSymbol>() != null) return
        if (!declaration.isLocal && typeArgumentSymbol.isLocal && origin.isDataFrame) {
            reporter.reportOn(
                declaration.source,
                DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE,
                schema.toMaterializedSchema(declaration.name.identifier.toDataSchemaName(), asDataClass = true)
            )
        }
    }
}

object ShadowedExtensionPropertyChecker : FirPropertyAccessExpressionChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        val property = expression.toResolvedCallableReference()?.toResolvedPropertySymbol() ?: return
        if (property is FirLocalPropertySymbol && !property.origin.isDataFrame) {
            val schema = context.findClosest<FirAnonymousFunctionSymbol>()
                ?.resolvedReceiverType?.typeArguments?.getOrNull(0)
                ?.type?.toRegularClassSymbol()
                ?: return
            if (schema.isLocal && schema.origin.isDataFrame || schema.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, context.session)) {
                schema.unsubstitutedScope().processAllProperties {
                    if (property.name == it.name) {
                        reporter.reportOn(
                            expression.source,
                            DATAFRAME_EXTENSION_PROPERTY_SHADOWED
                        )
                    }
                }
            }
        }
    }
}

internal val FirDeclarationOrigin.isDataFrame get() = this is FirDeclarationOrigin.Plugin && this.key == DataFramePlugin
