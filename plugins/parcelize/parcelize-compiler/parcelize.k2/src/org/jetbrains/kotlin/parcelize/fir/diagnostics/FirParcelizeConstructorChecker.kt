/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.hasValOrVar
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirConstructorChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedValueParameterSymbol
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.parcelize.ParcelizeNames

class FirParcelizeConstructorChecker(
    private val parcelizeAnnotations: List<ClassId>,
    private val experimentalCodeGeneration: Boolean
) : FirConstructorChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirConstructor, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isPrimary) return
        val source = declaration.source ?: return
        if (source.kind == KtFakeSourceElementKind.ImplicitConstructor) return
        val containingClass = context.containingDeclarations.last() as? FirRegularClass ?: return
        val containingClassSymbol = containingClass.symbol
        if (!containingClassSymbol.isParcelize(context.session, parcelizeAnnotations)
            || containingClass.hasCustomParceler(context.session)) {
            return
        }
        if (declaration.valueParameters.isEmpty()) {
            reporter.reportOn(containingClass.source, KtErrorsParcelize.PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY, context)
            return
        }
        val notValOrVarParameters = mutableListOf<FirValueParameter>()
        for (valueParameter in declaration.valueParameters) {
            if (valueParameter.source?.hasValOrVar() != true) {
                notValOrVarParameters.add(valueParameter)
                continue
            }
            if (valueParameter.defaultValue == null) {
                val illegalAnnotation = valueParameter.correspondingProperty?.annotations?.firstOrNull {
                    it.toAnnotationClassId(context.session) in ParcelizeNames.IGNORED_ON_PARCEL_CLASS_IDS
                }
                if (illegalAnnotation != null) {
                    reporter.reportOn(
                        illegalAnnotation.source,
                        KtErrorsParcelize.INAPPLICABLE_IGNORED_ON_PARCEL_CONSTRUCTOR_PROPERTY,
                        context
                    )
                }
            }
        }
        val superIsParcelize = containingClass.superTypeRefs.any {
            it.toRegularClassSymbol(context.session)?.isParcelize(context.session, parcelizeAnnotations) == true
        }
        val allowBareValueArguments = experimentalCodeGeneration
                && superIsParcelize
                && !containingClassSymbol.hasParcelerCompanionInChain(context.session)
        if (allowBareValueArguments) {
            val lookingFor = notValOrVarParameters.map { it.symbol }.toSet()
            val referenceFinder = ReferenceFinder(lookingFor, reporter, context)
            // check if they are referenced in the bodies, if so report error
            @OptIn(DirectDeclarationsAccess::class)
            for (decl in containingClass.declarations) when (decl) {
                is FirAnonymousInitializer, is FirProperty -> decl.accept(referenceFinder)
                else -> {}
            }
        } else {
            for (valueParameter in notValOrVarParameters) {
                reporter.reportOn(
                    valueParameter.source,
                    KtErrorsParcelize.PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR,
                    context
                )
            }
        }

    }

    private fun FirRegularClassSymbol.hasParcelerCompanionInChain(session: FirSession): Boolean {
        if (!isParcelize(session, parcelizeAnnotations)) return false
        return hasCustomParceler(session) || this.resolvedSuperTypeRefs.any {
            it.toRegularClassSymbol(session)?.hasParcelerCompanionInChain(session) == true
        }
    }

    private fun FirRegularClassSymbol.hasCustomParceler(session: FirSession): Boolean {
        val companion = companionObjectSymbol ?: return false
        return lookupSuperTypes(companion, lookupInterfaces = true, deep = true, useSiteSession = session).any {
            it.classId in ParcelizeNames.PARCELER_CLASS_IDS
        }
    }
}

class ReferenceFinder(
    private val lookingFor: Set<FirValueParameterSymbol>,
    private val reporter: DiagnosticReporter,
    private val context: CheckerContext,
) : FirVisitorVoid() {

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
        super.visitPropertyAccessExpression(propertyAccessExpression)
        val asValueParameter = propertyAccessExpression.calleeReference.toResolvedValueParameterSymbol(discardErrorReference = true)
        if (asValueParameter != null && asValueParameter in lookingFor) {
            reporter.reportOn(
                propertyAccessExpression.source, KtErrorsParcelize.VALUE_PARAMETER_USED_IN_CLASS_BODY, context
            )
        }
    }
}

