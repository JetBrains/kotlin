/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlinx.dataframe.plugin.extensions.ImportedSchemasDiagnostics.INVALID_SUPERTYPE
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

class ImportedSchemasCheckers(
    session: FirSession,
) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers: Set<FirRegularClassChecker>
            get() = setOfNotNull(
                ImportedSchemaCompanionObjectChecker,
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
                    if (it.classId == Names.DATAFRAME_PROVIDER && argument != null && argument.toRegularClassSymbol() != containingClassSymbol) {
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

object ImportedSchemasDiagnostics : KtDiagnosticsContainer() {
    val CONFLICTING_COMPANION_OBJECT_DECLARATION by error1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)
    val INVALID_SUPERTYPE by error1<KtElement, String>(SourceElementPositioningStrategies.SUPERTYPES_LIST)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = ImportedSchemaDiagnosticRenderers

    private object ImportedSchemaDiagnosticRenderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("DataFrame Imported Schemas") {
            it.put(CONFLICTING_COMPANION_OBJECT_DECLARATION, "{0}", TO_STRING)
            it.put(INVALID_SUPERTYPE, "{0}", TO_STRING)
        }
    }
}