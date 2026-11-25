/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.checkers.isPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.hasReceiverOrContextParameters
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField
import org.jetbrains.kotlin.lombok.generators.kotlin.promotedPropertiesByName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds

object FirLombokBuilderFunctionChecker : FirFunctionChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        val lombokService = context.session.lombokService
        val functionSymbol = declaration.symbol
        val containingClassSymbol = declaration.getContainingClassSymbol() as? FirRegularClassSymbol ?: return
        val classBuilder = lombokService.getBuilder(containingClassSymbol) ?: lombokService.getSuperBuilder(containingClassSymbol)
        // `@SuperBuilder` only allows `TYPE` as a target, so only plain `@Builder` can land on a constructor or on a function
        val functionBuilder = lombokService.getBuilder(functionSymbol)
        if (functionBuilder == null && classBuilder == null) return

        if (functionBuilder != null && functionSymbol.hasReceiverOrContextParameters) {
            // Nothing is generated for such a declaration, so the checks below have nothing to say about it.
            reporter.reportOn(
                functionBuilder.annotation.source,
                LombokFirDiagnostics.BUILDER_WITH_RECEIVER_OR_CONTEXT_PARAMETERS,
                context,
            )
            return
        }

        // A constructor builder instantiates the very class the constructor belongs to, so a class that
        // `build()` cannot instantiate refuses one exactly as it refuses the class-level annotation - and,
        // again, nothing is generated for it, leaving the checks below nothing to say. A function builder
        // is untouched by this: it builds whatever the function returns, which need not be this class.
        if (functionBuilder != null && functionSymbol is FirConstructorSymbol) {
            UNINSTANTIABLE_CLASS_MODIFIERS.firstOrNull { containingClassSymbol.hasModifier(it) }?.let { modifier ->
                val annotationName = functionBuilder.annotation.toAnnotationClassId(context.session)?.shortClassName
                if (annotationName != null) {
                    withSyntax {
                        reporter.reportOn(
                            functionBuilder.annotation.source,
                            LombokFirDiagnostics.ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS,
                            annotationName,
                            modifier.name,
                            context,
                        )
                    }
                }
                return
            }
        }

        checkFunctionParameters(containingClassSymbol, functionSymbol, classBuilder, functionBuilder)
        if (functionBuilder != null && functionSymbol is FirNamedFunctionSymbol) {
            checkBuilderFunctionHasExplicitTypeRef(functionSymbol, functionBuilder)
        }
    }

    private val UNINSTANTIABLE_CLASS_MODIFIERS = setOf(
        KotlinSyntaxTokenIds.INNER_MODIFIER,
        KotlinSyntaxTokenIds.ABSTRACT_MODIFIER,
        KotlinSyntaxTokenIds.SEALED_MODIFIER
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkBuilderFunctionHasExplicitTypeRef(function: FirNamedFunctionSymbol, builder: ConeLombokAnnotations.Builder) {
        if (function.resolvedReturnTypeRef.source?.kind != KtFakeSourceElementKind.ImplicitTypeRef) return

        reporter.reportOn(builder.annotation.source, LombokFirDiagnostics.BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE, context)
    }

    /**
     * `@Builder.Default` is `@Target(FIELD)`, so it can never land on a bare constructor parameter
     * (Kotlin's own annotation-target checker rejects that) — only `@Singular` (`@Target(FIELD, PARAMETER)`)
     * is possible here. A parameter's own default value (`= expr`) is still flagged as ignored, the same
     * way a plain property initializer is for a class-level `@Builder`.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFunctionParameters(
        classSymbol: FirRegularClassSymbol,
        function: FirFunctionSymbol<*>,
        // At least one of builders must be non-null
        classBuilder: ConeLombokAnnotations.AbstractBuilder?,
        functionBuilder: ConeLombokAnnotations.Builder?,
    ) {
        val isPrimaryConstructor = function.isPrimaryConstructor()
        val promotedProperties = if (isPrimaryConstructor) classSymbol.promotedPropertiesByName() else emptyMap()
        val declaredPropertyNames = classSymbol.declaredPropertyNames()
        val isPrimaryConstructorInBuilderCapableClass = isPrimaryConstructor && classSymbol.isBuilderCapableClass
        for (parameterSymbol in function.valueParameterSymbols) {
            // See the same guard in `checkClassProperties`: a parameter without a name is left alone.
            if (parameterSymbol.name.isSpecial) continue

            val property = promotedProperties[parameterSymbol.name]
            if (!isPrimaryConstructorInBuilderCapableClass || property == null) {
                // This check is mostly required for parameters.
                // Checking properties of a primary constructor is an exceptional situation bound to builder-incapable classes.
                checkDefaultAndSingular(parameterSymbol, property)
            }

            val explicitInitializerSource = parameterSymbol.resolvedDefaultValueSource
            val defaultAnnotation = property?.findAnnotationOnPropertyOrField(LombokNames.BUILDER_DEFAULT_ID)
            if (explicitInitializerSource != null && (functionBuilder != null || isPrimaryConstructorInBuilderCapableClass && defaultAnnotation == null)) {
                // Primary constructor properties have to hold `@Builder.Default` to use default values
                // Function parameters cannot hold `@Builder.Default` at all
                reporter.reportOn(explicitInitializerSource, LombokFirDiagnostics.BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION, context)
            }
            if (explicitInitializerSource == null && isPrimaryConstructorInBuilderCapableClass && defaultAnnotation != null) {
                // `@Builder.Default` requires a default value
                reporter.reportOn(defaultAnnotation.source, LombokFirDiagnostics.BUILDER_DEFAULT_REQUIRES_INITIALIZING_EXPRESSION, context)
            }
            if (parameterSymbol.name !in declaredPropertyNames) {
                // If we don't have a property matching a parameter name, `@Builder(toBuilder = true)` cannot obtain it.
                // TODO: implement `@Builder.ObtainVia` as another way to obtain a value (KT-89186)
                if (functionBuilder?.requiresToBuilder == true && !classSymbol.isCompanion ||
                    classBuilder?.requiresToBuilder == true && isPrimaryConstructorInBuilderCapableClass
                ) {
                    reporter.reportOn(parameterSymbol.source, LombokFirDiagnostics.TO_BUILDER_CANNOT_OBTAIN, parameterSymbol.name, context)
                }
            }
        }
    }

    /**
     * The names of the properties [this] class itself declares, promoted from the primary constructor or not:
     * what `BuilderBodyBuilder.buildToBuilder` looks a builder field up among, and so what decides whether it
     * has a value to copy.
     */
    context(context: CheckerContext)
    private fun FirRegularClassSymbol.declaredPropertyNames(): Set<Name> = buildSet {
        context.session.declaredMemberScope(this@declaredPropertyNames, memberRequiredPhase = null).processAllProperties { variableSymbol ->
            if (variableSymbol is FirPropertySymbol) add(variableSymbol.name)
        }
    }
}
