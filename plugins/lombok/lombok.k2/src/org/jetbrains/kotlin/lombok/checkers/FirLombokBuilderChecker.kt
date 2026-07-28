/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.config.LombokService
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.Singulars
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField

object FirLombokBuilderChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val lombokService = context.session.lombokService
        val hasBuilder = lombokService.getBuilder(declaration.symbol) != null ||
                lombokService.getSuperBuilder(declaration.symbol) != null
        if (!hasBuilder) return

        val declaredMemberScope = context.session.declaredMemberScope(declaration.symbol, memberRequiredPhase = null)
        declaredMemberScope.processAllProperties { variableSymbol ->
            val property = variableSymbol as? FirPropertySymbol ?: return@processAllProperties
            if (!property.hasBackingField) return@processAllProperties

            val singularAnnotation = property.findAnnotationOnPropertyOrField(LombokNames.SINGULAR_ID, context.session)
            val defaultAnnotation = property.findAnnotationOnPropertyOrField(LombokNames.BUILDER_DEFAULT_ID, context.session)

            if (singularAnnotation != null) {
                checkSingular(property, singularAnnotation, lombokService)

                if (defaultAnnotation != null) {
                    reporter.reportOn(defaultAnnotation.source, LombokFirDiagnostics.BUILDER_DEFAULT_AND_SINGULAR_MIXED, context)
                }
            }

            val explicitInitializerSource = property.explicitInitializerSource()
            if (explicitInitializerSource != null) {
                if (defaultAnnotation == null) {
                    reporter.reportOn(explicitInitializerSource, LombokFirDiagnostics.BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION, context)
                }
            } else if (defaultAnnotation != null) {
                reporter.reportOn(defaultAnnotation.source, LombokFirDiagnostics.BUILDER_DEFAULT_REQUIRES_INITIALIZING_EXPRESSION, context)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSingular(property: FirPropertySymbol, singularAnnotation: FirAnnotation, lombokService: LombokService) {
        val singular = ConeLombokAnnotations.Singular.extract(singularAnnotation, context.session)
        val source = singularAnnotation.source

        if (singular.singularName == null) {
            if (!lombokService.config.singularAuto) {
                reporter.reportOn(source, LombokFirDiagnostics.SINGULAR_REQUIRES_EXPLICIT_NAME, context)
            } else if (Singulars.autoSingularize(property.name.identifier) == null) {
                reporter.reportOn(source, LombokFirDiagnostics.CANNOT_SINGULARIZE_NAME, context)
            }
        }

        val typeName = property.resolvedReturnType.classId?.asFqNameString()
        if (typeName != null &&
            typeName !in LombokNames.SUPPORTED_COLLECTIONS &&
            typeName !in LombokNames.SUPPORTED_MAPS &&
            typeName !in LombokNames.SUPPORTED_TABLES
        ) {
            reporter.reportOn(source, LombokFirDiagnostics.UNSUPPORTED_SINGULAR_TYPE, typeName, context)
        }
    }

    /**
     * Properties promoted from primary constructor parameters always have a synthetic
     * initializer that reads the parameter's value, regardless of whether the parameter
     * itself declares a default value (`= expr`). Only the parameter's own default value
     * (or, for non-constructor properties, the property's own initializer) reflects what
     * the user actually wrote.
     */
    private fun FirPropertySymbol.explicitInitializerSource(): KtSourceElement? {
        val parameter = correspondingValueParameterFromPrimaryConstructor
        return if (parameter != null) parameter.defaultValueSource else initializerSource
    }
}
