/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCEL_TAG_CLASS_IDS
import org.jetbrains.kotlin.parcelize.ParcelizeNames.POLYMORPHIC_SEALED_CLASS_IDS
import org.jetbrains.kotlin.parcelize.fir.parcelizeService

object FirPolymorphicSealedClassChecker : FirClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        checkPolymorphicSealedDeclaration(declaration, context, reporter)
        checkPolymorphicSealedSubclass(declaration, context, reporter)
    }

    private fun checkPolymorphicSealedDeclaration(klass: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!klass.symbol.isPolymorphicSealed(context.session)) return

        val source = klass.source ?: return

        if (klass !is FirRegularClass || !klass.isSealed) {
            reporter.reportOn(source, KtErrorsParcelize.POLYMORPHIC_SEALED_MUST_BE_SEALED, context)
            return
        }

        val parcelizeAnnotations = context.session.parcelizeService.parcelizeAnnotations
        val hasParcelize = klass.symbol.resolvedAnnotationsWithClassIds.any {
            it.toAnnotationClassId(context.session) in parcelizeAnnotations
        }
        if (!hasParcelize) {
            reporter.reportOn(source, KtErrorsParcelize.POLYMORPHIC_SEALED_WITHOUT_PARCELIZE, context)
            return
        }

        checkPolymorphicSealedTags(klass, context, reporter)
    }

    private fun checkPolymorphicSealedTags(
        sealedRoot: FirRegularClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val session = context.session
        val subclasses = sealedRoot.getSealedClassInheritors(session)
            .mapNotNull { session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol }

        if (subclasses.none { it.hasParcelTag(session) }) return

        for (sub in subclasses) {
            if (!sub.hasParcelTag(session)) {
                reporter.reportOn(sub.source, KtErrorsParcelize.INCONSISTENT_PARCEL_TAG, context)
            }
        }

        val seenTags = mutableSetOf<Int>()
        for (sub in subclasses) {
            val tagAnnotation = sub.getParcelTagAnnotation(session) ?: continue
            val tagValue = extractParcelTagValue(tagAnnotation) ?: continue
            if (!seenTags.add(tagValue)) {
                reporter.reportOn(tagAnnotation.source, KtErrorsParcelize.DUPLICATE_PARCEL_TAG, context)
            }
        }
    }

    private fun checkPolymorphicSealedSubclass(klass: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (klass !is FirRegularClass) return
        val source = klass.source ?: return
        val session = context.session

        val polymorphicSealedSupertypes = klass.superTypeRefs.mapNotNull { it.coneType.toRegularClassSymbol(session) }
            .filter { it.isPolymorphicSealed(session) }

        if (polymorphicSealedSupertypes.isEmpty()) return

        if (polymorphicSealedSupertypes.size > 1) {
            reporter.reportOn(source, KtErrorsParcelize.MULTIPLE_POLYMORPHIC_SEALED_SUPERTYPES, context)
            return
        }

        val containingClassSymbol = klass.symbol.getContainingClassSymbol()
        if (containingClassSymbol != polymorphicSealedSupertypes.single()) {
            reporter.reportOn(source, KtErrorsParcelize.POLYMORPHIC_SEALED_SUBCLASS_MUST_BE_NESTED, context)
        }

        if (klass.isSealed) {
            reporter.reportOn(source, KtErrorsParcelize.POLYMORPHIC_SEALED_CANNOT_HAVE_SEALED_SUBCLASSES, context)
            return
        }

        if (klass.symbol.rawStatus.modality == Modality.OPEN) {
            reporter.reportOn(source, KtErrorsParcelize.POLYMORPHIC_SEALED_CANNOT_HAVE_OPEN_SUBCLASSES, context)
        }

        if (klass.symbol.rawStatus.modality == Modality.ABSTRACT) {
            reporter.reportOn(source, KtErrorsParcelize.POLYMORPHIC_SEALED_CANNOT_HAVE_ABSTRACT_SUBCLASSES, context)
            return
        }
    }

    private fun FirClassSymbol<*>.hasParcelTag(session: FirSession): Boolean {
        return resolvedAnnotationsWithClassIds.any { it.toAnnotationClassId(session) in PARCEL_TAG_CLASS_IDS }
    }

    private fun FirClassSymbol<*>.getParcelTagAnnotation(session: FirSession): FirAnnotation? {
        return resolvedAnnotationsWithArguments.find { it.toAnnotationClassId(session) in PARCEL_TAG_CLASS_IDS }
    }

    fun FirClassSymbol<*>.isPolymorphicSealed(session: FirSession): Boolean {
        return resolvedAnnotationsWithClassIds.any { it.toAnnotationClassId(session) in POLYMORPHIC_SEALED_CLASS_IDS }
    }

    private fun extractParcelTagValue(annotation: FirAnnotation): Int? {
        val tagArg = annotation.findArgumentByName(Name.identifier("tag"))
        return (tagArg as? FirLiteralExpression)?.let { (it.value as? Number)?.toInt() }
    }
}
