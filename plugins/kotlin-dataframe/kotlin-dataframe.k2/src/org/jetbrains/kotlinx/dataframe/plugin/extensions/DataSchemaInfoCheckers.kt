/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaMetadata
import org.jetbrains.kotlinx.dataframe.plugin.extensions.SchemaInfoDiagnostics.GENERATED_FROM_SOURCE_SCHEMA
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names
import org.jetbrains.kotlinx.dataframe.plugin.utils.isDataFrame
import org.jetbrains.kotlinx.dataframe.plugin.utils.isDataRow
import org.jetbrains.kotlinx.dataframe.plugin.utils.isGroupBy

class DataSchemaInfoCheckers(
    session: FirSession,
    withImportedSchemasReader: Boolean
) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(
            FunctionCallSchemaReporter,
        )
        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = setOf(
            PropertyAccessSchemaReporter,
        )
    }
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> = setOf(
            PropertySchemaReporter,
        )
        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> =
            setOf(
                FunctionDeclarationSchemaReporter,
            )
        override val regularClassCheckers: Set<FirRegularClassChecker> = setOfNotNull(
            ImportedSchemaInfoChecker.takeIf { withImportedSchemasReader }
        )
    }
}

object SchemaInfoDiagnostics : KtDiagnosticsContainer() {
    val PROPERTY_SCHEMA by info1(SourceElementPositioningStrategies.DECLARATION_NAME)
    val FUNCTION_SCHEMA by info1(SourceElementPositioningStrategies.DECLARATION_SIGNATURE)
    val FUNCTION_CALL_SCHEMA by info1(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val PROPERTY_ACCESS_SCHEMA by info1(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val GENERATED_FROM_SOURCE_SCHEMA by info1(SourceElementPositioningStrategies.DECLARATION_NAME)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = SchemaRenderers

    private object SchemaRenderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrame Schemas") {
            it.put(PROPERTY_SCHEMA, "{0}", TO_STRING)
            it.put(FUNCTION_SCHEMA, "{0}", TO_STRING)
            it.put(FUNCTION_CALL_SCHEMA, "{0}", TO_STRING)
            it.put(PROPERTY_ACCESS_SCHEMA, "{0}", TO_STRING)
            it.put(GENERATED_FROM_SOURCE_SCHEMA, "{0}", TO_STRING)
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

private data object FunctionDeclarationSchemaReporter : FirSimpleFunctionChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        val type = declaration.returnTypeRef.coneType
        context.sessionContext {
            reportSchema(reporter, declaration.source, SchemaInfoDiagnostics.FUNCTION_SCHEMA, type, context)
        }
    }
}

private object ImportedSchemaInfoChecker : FirRegularClassChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val topLevelMetadata: Map<Name, ImportedSchemaMetadata> = context.session.importedSchemasService.topLevelMetadata
        if (declaration.hasAnnotation(Names.DATA_SCHEMA_SOURCE_CLASS_ID, context.session)) {
            val metadata = topLevelMetadata[declaration.classId.shortClassName]
            context.sessionContext {
                val schema: PluginDataFrameSchema = pluginDataFrameSchema(declaration.defaultType())
                val message = buildString {
                    appendLine()
                    if (metadata != null) {
                        appendLine(metadata.format)
                    }
                }
                reporter.reportOn(declaration.source, GENERATED_FROM_SOURCE_SCHEMA, message + schema.toString())
            }
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
