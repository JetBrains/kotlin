/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
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

        val classHasBuilder = lombokService.getBuilder(declaration.symbol) != null ||
                lombokService.getSuperBuilder(declaration.symbol) != null
        if (classHasBuilder) {
            checkClassProperties(declaration, lombokService)
        }

        // `@SuperBuilder` only allows `TYPE` as a target, so only plain `@Builder` can land on a constructor
        // or on a function (a companion-object factory function, the Kotlin analogue of a Java static
        // factory method).
        declaration.processAllDeclarations(context.session) { symbol ->
            val functionSymbol = symbol as? FirFunctionSymbol<*> ?: return@processAllDeclarations
            val builder = lombokService.getBuilder(functionSymbol) ?: return@processAllDeclarations
            checkFunctionParameters(functionSymbol, lombokService)
            if (functionSymbol is FirNamedFunctionSymbol) {
                checkBuilderClassNameIsInferable(functionSymbol, builder)
            }
        }
    }

    /**
     * Unless it is spelled out via `builderClassName`, the builder class name is inferred from the annotated
     * function's return type. That name is needed as early as the SUPERTYPES stage, so it can only be read off
     * the *syntactic* return type (see `AbstractBuilderGenerator.getBuilderClassShortName`) — an implicitly
     * typed function leaves nothing to infer it from, and no builder would be generated at all.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkBuilderClassNameIsInferable(function: FirNamedFunctionSymbol, builder: ConeLombokAnnotations.Builder) {
        if (builder.hasSpecifiedBuilderClassName) return
        // So far a builder is only generated in full for a companion-object function; `@Builder` on any other
        // Kotlin function isn't supported yet, so there is nothing to demand a return type for. Drop this guard
        // once the remaining function kinds are supported.
        if (function.getContainingClassSymbol()?.isCompanion != true) return
        if (function.resolvedReturnTypeRef.source?.kind != KtFakeSourceElementKind.ImplicitTypeRef) return

        reporter.reportOn(builder.annotation.source, LombokFirDiagnostics.BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE, context)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkClassProperties(declaration: FirRegularClass, lombokService: LombokService) {
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

    /**
     * `@Builder.Default` is `@Target(FIELD)`, so it can never land on a bare constructor parameter
     * (Kotlin's own annotation-target checker rejects that) — only `@Singular` (`@Target(FIELD, PARAMETER)`)
     * is possible here. A parameter's own default value (`= expr`) is still flagged as ignored, the same
     * way a plain property initializer is for a class-level `@Builder`.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFunctionParameters(function: FirFunctionSymbol<*>, lombokService: LombokService) {
        for (parameterSymbol in function.valueParameterSymbols) {
            parameterSymbol.getAnnotationByClassId(LombokNames.SINGULAR_ID, context.session)?.let { singularAnnotation ->
                checkSingular(parameterSymbol, singularAnnotation, lombokService)
            }

            parameterSymbol.defaultValueSource?.let { defaultValueSource ->
                reporter.reportOn(defaultValueSource, LombokFirDiagnostics.BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION, context)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSingular(variable: FirVariableSymbol<*>, singularAnnotation: FirAnnotation, lombokService: LombokService) {
        val singular = ConeLombokAnnotations.Singular.extract(singularAnnotation, context.session)
        val source = singularAnnotation.source

        if (singular.singularName == null) {
            if (!lombokService.config.singularAuto) {
                reporter.reportOn(source, LombokFirDiagnostics.SINGULAR_REQUIRES_EXPLICIT_NAME, context)
            } else if (Singulars.autoSingularize(variable.name.identifier) == null) {
                reporter.reportOn(source, LombokFirDiagnostics.CANNOT_SINGULARIZE_NAME, context)
            }
        }

        val classId = variable.resolvedReturnType.classId
        if (classId != null &&
            classId !in LombokNames.SUPPORTED_COLLECTION_IDS &&
            classId !in LombokNames.SUPPORTED_MAP_IDS &&
            classId !in LombokNames.SUPPORTED_TABLE_IDS
        ) {
            reporter.reportOn(source, LombokFirDiagnostics.UNSUPPORTED_SINGULAR_TYPE, variable.resolvedReturnType, context)
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
