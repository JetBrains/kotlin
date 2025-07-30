/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaMetadata
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ImportedSchemasDiagnostics.GENERATED_FROM_SOURCE_SCHEMA
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ImportedSchemasDiagnostics.INVALID_SUPERTYPE
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class ImportedSchemasCheckers(
    session: FirSession,
    dumpSchemas: Boolean,
) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers: Set<FirRegularClassChecker>
            get() = setOfNotNull(
                ImportedSchemaCompanionObjectChecker,
                ImportedSchemaInfoChecker().takeIf { dumpSchemas }
            )
    }
}

private object ImportedSchemaCompanionObjectChecker : FirRegularClassChecker(mppKind = MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val containingClassSymbol = declaration.getContainingClassSymbol()
        if (declaration.isCompanion && declaration.origin !is FirDeclarationOrigin.Plugin && containingClassSymbol != null) {
            val className = containingClassSymbol.name
            if (declaration.symbol.resolvedSuperTypes.none { it.classId == Names.DATAFRAME_PROVIDER }) {
                reporter.reportOn(
                    declaration.source,
                    ImportedSchemasDiagnostics.CONFLICTING_COMPANION_OBJECT_DECLARATION,
                    "Declaration conflicts with plugin-generated companion object. Add `: ${Names.DATAFRAME_PROVIDER.shortClassName}<${className}>` supertype to resolve the conflict, or remove companion object."
                )
            } else {
                declaration.symbol.resolvedSuperTypes.forEach {
                    val argument = it.typeArguments.firstOrNull() as? ConeKotlinType
                    if (it.classId == Names.DATAFRAME_PROVIDER && argument != null && argument.toRegularClassSymbol(context.session) != containingClassSymbol) {
                        reporter.reportOn(
                            declaration.source,
                            INVALID_SUPERTYPE,
                            "Expected type argument of ${Names.DATAFRAME_PROVIDER.shortClassName}: ${className}. Actual: $argument"
                        )
                    }
                }
            }
        }
    }
}

private class ImportedSchemaInfoChecker() : FirRegularClassChecker(mppKind = MppCheckerKind.Common) {

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

object ImportedSchemasDiagnostics : KtDiagnosticsContainer() {
    val GENERATED_FROM_SOURCE_SCHEMA by info1(SourceElementPositioningStrategies.DECLARATION_NAME)
    val CONFLICTING_COMPANION_OBJECT_DECLARATION by error1<PsiElement, String>(SourceElementPositioningStrategies.DEFAULT)
    val INVALID_SUPERTYPE by error1<PsiElement, String>(SourceElementPositioningStrategies.SUPERTYPES_LIST)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = SchemaRenderers

    private object SchemaRenderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrame Schemas") {
            it.put(GENERATED_FROM_SOURCE_SCHEMA, "{0}", TO_STRING)
            it.put(INVALID_SUPERTYPE, "{0}", TO_STRING)
        }
    }
}