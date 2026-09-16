/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
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
import org.jetbrains.kotlin.lombok.generators.hasReceiverOrContextParameters
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField
import org.jetbrains.kotlin.lombok.generators.kotlin.promotedPropertiesByName
import org.jetbrains.kotlin.name.Name

object FirLombokBuilderChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    private val BUILDER_FIELD_ANNOTATION_IDS = listOf(LombokNames.BUILDER_DEFAULT_ID, LombokNames.SINGULAR_ID)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val lombokService = context.session.lombokService

        checkBuilderFieldAnnotationsOnBodyProperties(declaration)

        val classBuilder = lombokService.getBuilder(declaration.symbol) ?: lombokService.getSuperBuilder(declaration.symbol)
        // A Java class builds out of its fields and so is never short of anything to build from; only a Kotlin
        // class goes through the primary constructor, exactly as `AbstractBuilderGenerator` splits the two.
        if (classBuilder != null && declaration !is FirJavaClass && declaration.isBuilderCapableClass) {
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
            } else {
                checkPrimaryConstructorParameters(declaration, primaryConstructor, lombokService)
                checkToBuilderCanObtainValues(declaration, classBuilder, primaryConstructor)
            }
        }

        // `@SuperBuilder` only allows `TYPE` as a target, so only plain `@Builder` can land on a constructor
        // or on a function (a companion-object factory function, the Kotlin analogue of a Java static
        // factory method).
        declaration.processAllDeclarations(context.session) { symbol ->
            val functionSymbol = symbol as? FirFunctionSymbol<*> ?: return@processAllDeclarations
            val builder = lombokService.getBuilder(functionSymbol) ?: return@processAllDeclarations

            if (functionSymbol.hasReceiverOrContextParameters) {
                // Nothing is generated for such a declaration, so the checks below have nothing to say about it.
                reporter.reportOn(
                    builder.annotation.source,
                    LombokFirDiagnostics.BUILDER_WITH_RECEIVER_OR_CONTEXT_PARAMETERS,
                    context,
                )
                return@processAllDeclarations
            }

            // A constructor builder instantiates the very class the constructor belongs to, so a class that
            // `build()` cannot instantiate refuses one exactly as it refuses the class-level annotation - and,
            // again, nothing is generated for it, leaving the checks below nothing to say. A function builder
            // is untouched by this: it builds whatever the function returns, which need not be this class.
            if (functionSymbol is FirConstructorSymbol) {
                declaration.uninstantiableClassModifier()?.let { modifier ->
                    val annotationName = builder.annotation.toAnnotationClassId(context.session)?.shortClassName
                    if (annotationName != null) {
                        reporter.reportOn(
                            builder.annotation.source,
                            LombokFirDiagnostics.ANNOTATION_IS_NOT_SUPPORTED_ON_CLASS,
                            annotationName,
                            modifier.presentation,
                            context,
                        )
                    }
                    return@processAllDeclarations
                }
            }

            checkFunctionParameters(functionSymbol, lombokService)
            checkToBuilderCanObtainValues(declaration, builder, functionSymbol)
            if (functionSymbol is FirNamedFunctionSymbol) {
                checkBuilderClassNameIsInferable(functionSymbol, builder)
            }
        }
    }

    /**
     * `toBuilder()` fills each builder field from the entity's property of that name - see
     * `BuilderBodyBuilder.buildToBuilder` - and a parameter the class declares no property for leaves it with
     * nothing to read. Lombok rejects the same shape outright, with "cannot find symbol: variable <name>" on
     * the annotation: `@Builder(toBuilder = true)` on a method or constructor requires every parameter to be
     * obtainable, either from a field of that name or through `@Builder.ObtainVia`, which is not implemented
     * here (KT-89186). Silently generating a `toBuilder()` that drops the field is worse than refusing it, since the
     * round-trip it exists for quietly returns a different object.
     *
     * A class-level `@Builder` on a Java class cannot run into this - every builder field is a field of the
     * class - but a Kotlin one can, its builder fields being the primary constructor's value parameters,
     * `val` or not.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkToBuilderCanObtainValues(
        declaration: FirRegularClass,
        builder: ConeLombokAnnotations.AbstractBuilder,
        builderDeclaration: FirFunctionSymbol<*>,
    ) {
        if (!builder.requiresToBuilder || declaration.symbol.isCompanion) return

        val declaredPropertyNames = declaration.declaredPropertyNames()
        for (parameter in builderDeclaration.valueParameterSymbols) {
            // A parameter the parser could not read a name off gets no builder field either, see the generator.
            if (parameter.name.isSpecial || parameter.name in declaredPropertyNames) continue

            reporter.reportOn(parameter.source, LombokFirDiagnostics.TO_BUILDER_CANNOT_OBTAIN, parameter.name, context)
        }
    }

    private val FirRegularClass.isBuilderCapableClass: Boolean
        get() = classKind == ClassKind.CLASS && !isLocal && uninstantiableClassModifier() == null

    /**
     * `@Builder.Default` and `@Singular` shape a builder field, and a Kotlin class's builder fields are the value
     * parameters of the constructor or function that `build()` calls. A property declared in the class body is
     * therefore never one.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkBuilderFieldAnnotationsOnBodyProperties(declaration: FirRegularClass) {
        declaration.processAllDeclarations(context.session) { symbol ->
            val property = symbol as? FirPropertySymbol ?: return@processAllDeclarations
            // A promoted property *is* a primary constructor value parameter, and so is a builder field - the
            // one shape both annotations are for. `checkPrimaryConstructorParameters` validates those.
            if (property.fromPrimaryConstructor) return@processAllDeclarations

            for (annotationClassId in BUILDER_FIELD_ANNOTATION_IDS) {
                val annotation = property.findAnnotationOnPropertyOrField(annotationClassId, context.session) ?: continue
                reporter.reportOn(
                    annotation.source,
                    LombokFirDiagnostics.BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY,
                    annotationClassId.relativeClassName.asString(),
                    context,
                )
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
        if (function.resolvedReturnTypeRef.source?.kind != KtFakeSourceElementKind.ImplicitTypeRef) return

        reporter.reportOn(builder.annotation.source, LombokFirDiagnostics.BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE, context)
    }

    /**
     * A class-level `@Builder` builds out of the primary constructor's value parameters and nothing else - see
     * `AbstractBuilderGenerator`, whose `build()` has that constructor to call and no other way to reach a
     * property. A property declared in the class body is therefore not a builder field, so `@Singular`,
     * `@Builder.Default` and an initializer on one have nothing to do with the builder and are left alone here.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPrimaryConstructorParameters(
        declaration: FirRegularClass,
        primaryConstructor: FirConstructorSymbol,
        lombokService: LombokService,
    ) {
        val promotedProperties = declaration.promotedPropertiesByName()

        for (parameter in primaryConstructor.valueParameterSymbols) {
            // `AbstractBuilderGenerator` builds nothing out of a property the parser could not read a name off,
            // so nothing here has anything to report about one either.
            if (parameter.name.isSpecial) continue

            // The same lookup the generator does: `@Singular` is on the parameter unless it was written with a
            // `@field:` target, and `@Builder.Default` (`@Target(FIELD)`) is only ever on the promoted property.
            val property = promotedProperties[parameter.name]
            val singularAnnotation = parameter.getAnnotationByClassId(LombokNames.SINGULAR_ID, context.session)
                ?: property?.findAnnotationOnPropertyOrField(LombokNames.SINGULAR_ID, context.session)
            val defaultAnnotation = property?.findAnnotationOnPropertyOrField(LombokNames.BUILDER_DEFAULT_ID, context.session)

            if (singularAnnotation != null) {
                checkSingular(parameter, singularAnnotation, lombokService)

                if (defaultAnnotation != null) {
                    reporter.reportOn(defaultAnnotation.source, LombokFirDiagnostics.BUILDER_DEFAULT_AND_SINGULAR_MIXED, context)
                }
            }

            // The parameter's own default value (`= expr`) is what the user wrote: a promoted property always
            // carries a synthetic initializer reading the parameter, whether or not the parameter defaults.
            val explicitInitializerSource = parameter.resolvedDefaultValueSource
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
            // See the same guard in `checkClassProperties`: a parameter without a name is left alone.
            if (parameterSymbol.name.isSpecial) continue

            parameterSymbol.getAnnotationByClassId(LombokNames.SINGULAR_ID, context.session)?.let { singularAnnotation ->
                checkSingular(parameterSymbol, singularAnnotation, lombokService)
            }

            parameterSymbol.resolvedDefaultValueSource?.let { defaultValueSource ->
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
