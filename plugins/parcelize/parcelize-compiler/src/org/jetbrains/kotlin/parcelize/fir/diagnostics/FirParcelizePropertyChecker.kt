/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.classId
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.IGNORED_ON_PARCEL_CLASS_IDS
import org.jetbrains.kotlin.parcelize.PARCELER_CLASS_ID

object FirParcelizePropertyChecker : FirPropertyChecker() {
    private val CREATOR_NAME = Name.identifier("CREATOR")

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingClassSymbol = declaration.dispatchReceiverType?.toRegularClassSymbol(context.session) ?: return

        if (containingClassSymbol.isParcelize(context.session)) {
            val fromPrimaryConstructor = declaration.fromPrimaryConstructor ?: false
            if (
                !fromPrimaryConstructor &&
                (declaration.hasBackingField || declaration.delegate != null) &&
                !declaration.hasIgnoredOnParcel()
            ) {
                reporter.reportOn(declaration.source, KtErrorsParcelize.PROPERTY_WONT_BE_SERIALIZED, context)
            }
            if (fromPrimaryConstructor) {
                checkParcelableClassProperty(declaration, containingClassSymbol, context, reporter)
                checkIgnoredOnParcelUsage(declaration, context, reporter)
            }
        }

        if (declaration.name == CREATOR_NAME && containingClassSymbol.isCompanion && declaration.hasJvmFieldAnnotation) {
            val outerClass = context.containingDeclarations.asReversed().getOrNull(1) as? FirRegularClass
            if (outerClass != null && outerClass.symbol.isParcelize(context.session)) {
                reporter.reportOn(declaration.source, KtErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED, context)
            }
        }
    }

    private fun checkIgnoredOnParcelUsage(property: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val illegalAnnotation = property.annotations.firstOrNull { it.classId in IGNORED_ON_PARCEL_CLASS_IDS } ?: return
        reporter.reportOn(illegalAnnotation.source, KtErrorsParcelize.INAPPLICABLE_IGNORED_ON_PARCEL_CONSTRUCTOR_PROPERTY, context)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkParcelableClassProperty(
        property: FirProperty,
        containingClassSymbol: FirRegularClassSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val type = property.returnTypeRef.coneType
        if (type is ConeKotlinErrorType || containingClassSymbol.hasCustomParceler(context.session)) return
        /*
         * TODO: abstract code from ParcelSerializer or IrParcelSerializerFactory to avoid duplication
         *    of allowed types checking
         */
    }

    private fun FirProperty.hasIgnoredOnParcel(): Boolean {
        return annotations.hasIgnoredOnParcel() || (getter?.annotations?.hasIgnoredOnParcel() ?: false)
    }


    private fun List<FirAnnotation>.hasIgnoredOnParcel(): Boolean {
        return this.any {
            if (it.annotationTypeRef.coneType.classId !in IGNORED_ON_PARCEL_CLASS_IDS) return@any false
            val target = it.useSiteTarget
            target == null || target == AnnotationUseSiteTarget.PROPERTY || target == AnnotationUseSiteTarget.PROPERTY_GETTER
        }
    }

    private fun FirRegularClassSymbol.hasCustomParceler(session: FirSession): Boolean {
        val companionObjectSymbol = this.companionObjectSymbol ?: return false
        return lookupSuperTypes(companionObjectSymbol, lookupInterfaces = true, deep = true, session).any {
            it.classId == PARCELER_CLASS_ID
        }
    }
}
