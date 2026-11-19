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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.checkers.isPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.hasReceiverOrContextParameters
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField
import org.jetbrains.kotlin.lombok.generators.kotlin.promotedPropertiesByName
import org.jetbrains.kotlin.name.Name

object FirLombokBuilderClassChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val lombokService = context.session.lombokService

        val classBuilder = lombokService.getBuilder(declaration.symbol) ?: lombokService.getSuperBuilder(declaration.symbol)
        if (classBuilder != null && declaration.isBuilderCapableClass) {
            val primaryConstructor = declaration.primaryConstructorIfAny(context.session)
            if (primaryConstructor == null) {
                val annotationName = classBuilder.annotation.toAnnotationClassId(context.session)?.shortClassName
                if (annotationName != null) {
                    reporter.reportOn(
                        classBuilder.annotation.source,
                        LombokFirDiagnostics.BUILDER_REQUIRES_PRIMARY_CONSTRUCTOR,
                        annotationName,
                        context,
                    )
                }
            }
        }

        // `@SuperBuilder` only allows `TYPE` as a target, so only plain `@Builder` can land on a constructor
        // or on a function (a companion-object factory function, the Kotlin analogue of a Java static
        // factory method).
        declaration.processAllDeclarations(context.session) { symbol ->
            val functionSymbol = symbol as? FirFunctionSymbol<*> ?: return@processAllDeclarations
            val functionBuilder = lombokService.getBuilder(functionSymbol)
            if (functionBuilder == null && classBuilder == null) return@processAllDeclarations

            if (functionBuilder != null && functionSymbol.hasReceiverOrContextParameters) {
                // Nothing is generated for such a declaration, so the checks below have nothing to say about it.
                reporter.reportOn(
                    functionBuilder.annotation.source,
                    LombokFirDiagnostics.BUILDER_WITH_RECEIVER_OR_CONTEXT_PARAMETERS,
                    context,
                )
                return@processAllDeclarations
            }

            // A constructor builder instantiates the very class the constructor belongs to, so a class that
            // `build()` cannot instantiate refuses one exactly as it refuses the class-level annotation - and,
            // again, nothing is generated for it, leaving the checks below nothing to say. A function builder
            // is untouched by this: it builds whatever the function returns, which need not be this class.
            if (functionBuilder != null && functionSymbol is FirConstructorSymbol) {
                UNINSTANTIABLE_CLASS_MODIFIERS.firstOrNull { declaration.hasModifier(it) }?.let { modifier ->
                    val annotationName = functionBuilder.annotation.toAnnotationClassId(context.session)?.shortClassName
                    if (annotationName != null) {
                        reporter.reportOn(
                            functionBuilder.annotation.source,
                            LombokFirDiagnostics.ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS,
                            annotationName,
                            modifier.value,
                            context,
                        )
                    }
                    return@processAllDeclarations
                }
            }

            checkFunctionParameters(declaration, functionSymbol, classBuilder, functionBuilder)
            if (functionBuilder != null && functionSymbol is FirNamedFunctionSymbol) {
                checkBuilderClassNameIsInferable(functionSymbol, functionBuilder)
            }
        }
    }

    private val UNINSTANTIABLE_CLASS_MODIFIERS = setOf(KtTokens.INNER_KEYWORD, KtTokens.ABSTRACT_KEYWORD, KtTokens.SEALED_KEYWORD)

    /**
     * Unless it is spelled out via `builderClassName`, the builder class name is inferred from the annotated
     * function's return type. That name is needed as early as the SUPERTYPES stage, so it can only be read off
     * the *syntactic* return type (see `AbstractBuilderGenerator.getBuilderClassShortName`) — an implicitly
     * typed function leaves nothing to infer it from, and no builder would be generated at all.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkBuilderClassNameIsInferable(function: FirNamedFunctionSymbol, builder: ConeLombokAnnotations.Builder) {
        if (builder.hasSpecifiedBuilderClassName) return
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
        declaration: FirRegularClass,
        function: FirFunctionSymbol<*>,
        // At least one of builders must be non-null
        classBuilder: ConeLombokAnnotations.AbstractBuilder?,
        functionBuilder: ConeLombokAnnotations.Builder?,
    ) {
        val isPrimaryConstructor = function.isPrimaryConstructor()
        val promotedProperties = if (isPrimaryConstructor) declaration.promotedPropertiesByName() else emptyMap()
        val declaredPropertyNames = declaration.declaredPropertyNames()
        val isPrimaryConstructorInBuilderCapableClass = isPrimaryConstructor && declaration.isBuilderCapableClass
        for (parameterSymbol in function.valueParameterSymbols) {
            // See the same guard in `checkClassProperties`: a parameter without a name is left alone.
            if (parameterSymbol.name.isSpecial) continue

            val property = promotedProperties[parameterSymbol.name]
            if (!isPrimaryConstructorInBuilderCapableClass || property == null) {
                checkDefaultAndSingular(parameterSymbol, property)
            }

            val explicitInitializerSource = parameterSymbol.resolvedDefaultValueSource
            val defaultAnnotation = property?.findAnnotationOnPropertyOrField(LombokNames.BUILDER_DEFAULT_ID)
            if (explicitInitializerSource != null && (functionBuilder != null || isPrimaryConstructorInBuilderCapableClass && defaultAnnotation == null)) {
                reporter.reportOn(explicitInitializerSource, LombokFirDiagnostics.BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION, context)
            }
            if (explicitInitializerSource == null && isPrimaryConstructorInBuilderCapableClass && defaultAnnotation != null) {
                reporter.reportOn(defaultAnnotation.source, LombokFirDiagnostics.BUILDER_DEFAULT_REQUIRES_INITIALIZING_EXPRESSION, context)
            }
            if (parameterSymbol.name !in declaredPropertyNames) {
                // If we don't have a property matching a parameter name, `@Builder(toBuilder = true)` cannot obtain it.
                // TODO: implement `@Builder.ObtainVia` as another way to obtain a value (KT-89186)
                if (functionBuilder?.requiresToBuilder == true && !declaration.symbol.isCompanion ||
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
    private fun FirRegularClass.declaredPropertyNames(): Set<Name> = buildSet {
        context.session.declaredMemberScope(symbol, memberRequiredPhase = null).processAllProperties { variableSymbol ->
            if (variableSymbol is FirPropertySymbol) add(variableSymbol.name)
        }
    }
}
