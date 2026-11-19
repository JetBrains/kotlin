/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.NotIncludedReason
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics.NotIncludedReason.HAS_IMPLICIT_RETURN_TYPE
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.AbstractBuilderGenerator
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object FirLombokBuilderPropertyChecker : FirPropertyChecker(MppCheckerKind.Platform) {
    private val BUILDER_FIELD_ANNOTATION_IDS = listOf(LombokNames.BUILDER_DEFAULT_ID, LombokNames.SINGULAR_ID)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val containingClassSymbol = declaration.getContainingClassSymbol() as? FirRegularClassSymbol ?: return
        if (!containingClassSymbol.isBuilderCapableClass) return
        val name = declaration.name.takeUnless { it.isSpecial } ?: return

        val fromPrimaryConstructor = declaration.fromPrimaryConstructor == true
        val primaryConstructor = containingClassSymbol.primaryConstructorIfAny(context.session)
        val parameter = runIf(fromPrimaryConstructor) {
            primaryConstructor?.valueParameterSymbols?.find {
                it.name == declaration.name
            }
        }
        checkDefaultAndSingular(parameter, declaration.symbol)

        if (fromPrimaryConstructor) return
        if (declaration.isVal) {
            // `@Builder.Default` and `@Singular` shape a builder field,
            // and a Kotlin class's builder fields are the value parameters of the constructor or function that `build()` calls,
            // as well as mutable member properties.
            for (annotationClassId in BUILDER_FIELD_ANNOTATION_IDS) {
                val annotation = declaration.symbol.findAnnotationOnPropertyOrField(annotationClassId) ?: continue
                reporter.reportOn(
                    annotation.source,
                    LombokFirDiagnostics.BUILDER_FIELD_ANNOTATION_ON_BODY_PROPERTY,
                    annotationClassId.relativeClassName.asString(),
                    context,
                )
            }
        }

        val lombokService = context.session.lombokService
        if (lombokService.getBuilder(containingClassSymbol) == null && lombokService.getSuperBuilder(containingClassSymbol) == null) return

        if (primaryConstructor == null) return
        val parameterNames = primaryConstructor.valueParameterSymbols.mapTo(mutableSetOf()) { it.name }
        val whyNotIncluded = with(AbstractBuilderGenerator) {
            declaration.symbol.whyNotIncludedInBuilder(parameterNames, resolveInProcess = false)
        }

        // A Kotlin class whose primary constructor has no value parameters builds out of the mutable properties declared
        // in its body instead - see `AbstractBuilderGenerator.calculateBuilderVariables`. The builder members are generated
        // before an implicit type is resolved, so `AbstractBuilderGenerator.addBuilderCallables` leaves out a property without
        // an explicit type, and its builder field and setter are silently missing (KT-90508).
        //
        // `build()` evaluates the initializer of a `@Builder.Default` body property itself when its setter was not called -
        // see `BuilderBodyBuilder.buildDefaultOrSetValue` - before the entity is constructed, so such an initializer
        // cannot use the entity instance (KT-90590). A constructor parameter's default value has no such restriction:
        // its references to earlier parameters are resolved to their already computed values.
        //
        // Any other property declared in the body - a read-only one, or any one at all when the primary constructor has
        // value parameters - is not a builder field, which is reported as well (KT-90879). A property without a backing field
        // holds no state of its own for a builder to set, and one named after a constructor parameter receives its value
        // from that parameter's builder field (and is what `toBuilder()` reads it back from), so both are left alone.
        if (whyNotIncluded != null && whyNotIncluded != NotIncludedReason.NOT_REPORTED) {
            val diagnostic = if (whyNotIncluded == HAS_IMPLICIT_RETURN_TYPE) {
                LombokFirDiagnostics.BUILDER_PROPERTY_NOT_INCLUDED_ERROR
            } else {
                LombokFirDiagnostics.BUILDER_PROPERTY_NOT_INCLUDED
            }
            reporter.reportOn(declaration.source, diagnostic, name, whyNotIncluded, context)
            return
        }

        if (declaration.symbol.findAnnotationOnPropertyOrField(LombokNames.BUILDER_DEFAULT_ID) == null) return
        val initializer = declaration.initializer ?: return
        if (initializer.usesInstanceOf(containingClassSymbol)) {
            reporter.reportOn(initializer.source, LombokFirDiagnostics.BUILDER_DEFAULT_INITIALIZER_USES_INSTANCE, context)
        }
    }

    /**
     * Whether [this] expression reads a `this` of [classSymbol], explicitly or as an implicit receiver. A lambda
     * counts even when it is not called in place: it still captures the instance when it is created, and `build()`
     * creates it with no instance to capture.
     */
    private fun FirExpression.usesInstanceOf(classSymbol: FirRegularClassSymbol): Boolean {
        var found = false
        accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (found) return
                if (element is FirThisReceiverExpression && element.calleeReference.boundSymbol == classSymbol) {
                    found = true
                    return
                }
                element.acceptChildren(this)
            }
        })
        return found
    }
}
