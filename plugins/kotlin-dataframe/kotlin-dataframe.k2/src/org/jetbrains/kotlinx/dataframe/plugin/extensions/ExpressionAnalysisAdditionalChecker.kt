/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isInline
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
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.CAST_ERROR
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.CAST_TARGET_WARNING
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATA_SCHEMA_DECLARATION_VISIBILITY
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.ERROR
import org.jetbrains.kotlinx.dataframe.plugin.extensions.FirDataFrameErrors.DATAFRAME_EXTENSION_PROPERTY_SHADOWED
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.flatten
import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.utils.ALLOWED_DECLARATION_VISIBILITY
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class ExpressionAnalysisAdditionalChecker(
    session: FirSession,
    isTest: Boolean,
) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOfNotNull(
            Checker(isTest),
            DataFrameFunctionCallTransformationContextChecker,
        )
        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = setOf(ShadowedExtensionPropertyChecker)
    }
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(DataSchemaDeclarationChecker)
        override val propertyCheckers: Set<FirPropertyChecker> = setOf(DataFramePropertyChecker)
    }
}

object FirDataFrameErrors : KtDiagnosticsContainer() {
    val ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)
    val CAST_ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val CAST_TARGET_WARNING by warning1<KtElement, String>(SourceElementPositioningStrategies.CALL_ELEMENT_WITH_DOT)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE by warning1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATA_SCHEMA_DECLARATION_VISIBILITY by error1<KtElement, String>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR by error1<KtElement, String>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE by error1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val DATAFRAME_EXTENSION_PROPERTY_SHADOWED by warning1<KtElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = DataFrameDiagnosticMessages
}

object DataFrameDiagnosticMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrameDiagnosticMessages") { map ->
        map.put(ERROR, "{0}", TO_STRING)
        map.put(CAST_ERROR, "{0}", TO_STRING)
        map.put(CAST_TARGET_WARNING, "{0}", TO_STRING)
        map.put(DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE, "{0}", TO_STRING)
        map.put(DATA_SCHEMA_DECLARATION_VISIBILITY, "{0}", TO_STRING)
        map.put(DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR, "{0}", TO_STRING)
        map.put(DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE, "{0}", TO_STRING)
        map.put(DATAFRAME_EXTENSION_PROPERTY_SHADOWED, "{0}", TO_STRING)
    }
}

private class Checker(
    val isTest: Boolean,
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
        val targetProjection = expression.typeArguments.getOrNull(0) as? FirTypeProjectionWithVariance ?: return
        val targetType = targetProjection.typeRef.coneType as? ConeClassLikeType ?: return
        val targetSymbol = targetType.toSymbol()
        if (targetSymbol != null && !session.predicateBasedProvider.matches(VALID_CAST_TARGET_PREDICATE, targetSymbol)) {
            val text = "Annotate ${targetType.renderReadable()} with @DataSchema to use generated properties"
            reporter.reportOn(expression.source, CAST_TARGET_WARNING, text, context)
        }
        val coneType = expression.explicitReceiver?.resolvedType
        if (coneType != null) {
            val sourceType = coneType.fullyExpandedType().typeArguments.getOrNull(0)?.type as? ConeClassLikeType
                ?: return
            val source = pluginDataFrameSchema(sourceType)
            if (source.columns().isEmpty()) return
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

internal object DataSchemaDeclarationChecker : FirRegularClassChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val annotated = declaration.hasAnnotation(Names.DATA_SCHEMA_CLASS_ID, context.session) ||
                declaration.hasAnnotation(Names.DATA_SCHEMA_SOURCE_CLASS_ID, context.session)
        if (annotated && declaration.effectiveVisibility !in ALLOWED_DECLARATION_VISIBILITY) {
            val visibilityOptions = ALLOWED_DECLARATION_VISIBILITY.joinToString(", ")
            reporter.reportOn(
                declaration.source,
                DATA_SCHEMA_DECLARATION_VISIBILITY,
                "To allow plugin-generated declarations to refer to this declaration, it must be declared as either of [$visibilityOptions]"
            )
        }
    }
}

private data object DataFrameFunctionCallTransformationContextChecker : FirFunctionCallChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        expression.toResolvedCallableReference()?.toResolvedNamedFunctionSymbol()?.let { symbol ->
            val shouldRefine = FunctionCallTransformer.shouldRefine(expression.annotations, symbol, context.session)
            if (shouldRefine && context.containingDeclarations.any { it is FirNamedFunctionSymbol && it.isInline }) {
                reporter.reportOn(
                    expression.source,
                    DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE,
                    "DataFrame compiler plugin is not yet supported in inline functions"
                )
            }

            if (shouldRefine && context.containingDeclarations.lastOrNull() is FirPropertyAccessorSymbol) {
                reporter.reportOn(
                    expression.source,
                    DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR,
                    "DataFrame compiler plugin is not yet supported in property accessors bodies. Use property with initializer or a function instead"
                )
            }
        }
    }
}

private data object DataFramePropertyChecker : FirPropertyChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val typeArgument =
            (declaration.symbol.resolvedReturnType.typeArguments.getOrNull(0) as? ConeClassLikeType)?.toRegularClassSymbol() ?: return
        val origin = typeArgument.origin
        if (context.findClosest<FirScriptSymbol>() != null) return
        if (!declaration.isLocal && typeArgument.isLocal && origin.isDataFrame) {
            reporter.reportOn(
                declaration.source,
                DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_RETURN_TYPE,
                "Local types produced by the DataFrame compiler plugin are not yet supported in property return types. Convert this property to a function or cast it to a DataSchema type."
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
                            DATAFRAME_EXTENSION_PROPERTY_SHADOWED,
                            "Extension property with implicit receiver is shadowed by a property with the same name."
                        )
                    }
                }
            }
        }
    }
}

internal val FirDeclarationOrigin.isDataFrame get() = this is FirDeclarationOrigin.Plugin && this.key == DataFramePlugin
