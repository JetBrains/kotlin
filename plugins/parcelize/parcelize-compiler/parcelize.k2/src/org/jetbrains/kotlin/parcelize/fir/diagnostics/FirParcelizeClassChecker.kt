/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATOR_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.OLD_PARCELER_ID
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELABLE_ID
import org.jetbrains.kotlin.parcelize.fir.isParcelize

object FirParcelizeClassChecker : FirClassChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        checkParcelableClass(declaration, context, reporter)
        checkParcelerClass(declaration, context, reporter)
    }

    private fun checkParcelableClass(klass: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = klass.symbol
        if (!symbol.isParcelize(context.session)) return
        val source = klass.source ?: return
        val classKind = klass.classKind

        when (klass) {
            is FirRegularClass -> {
                if (classKind == ClassKind.ANNOTATION_CLASS) {
                    reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_CANT_BE_ANNOTATION_CLASS, context)
                    return
                }

                if (classKind == ClassKind.INTERFACE && !klass.isSealed) {
                    reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_CANT_BE_NON_SEALED_INTERFACE, context)
                    return
                }

                klass.companionObjectSymbol?.let { companionSymbol ->
                    if (companionSymbol.classId.shortClassName == CREATOR_NAME) {
                        reporter.reportOn(companionSymbol.source, KtErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED, context)
                    }
                }

                if (klass.isInner) {
                    reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_CANT_BE_INNER_CLASS, context)
                }

                if (klass.isLocal) {
                    reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_CANT_BE_LOCAL_CLASS, context)
                }
            }
            is FirAnonymousObject -> {
                if (classKind != ClassKind.ENUM_ENTRY) {
                    reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_CANT_BE_ANONYMOUS_OBJECT, context)
                    return
                }
            }
        }

        if (classKind == ClassKind.CLASS && klass.isAbstract) {
            reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_SHOULD_BE_INSTANTIABLE, context)
        }

        val supertypes = lookupSuperTypes(klass, lookupInterfaces = true, deep = true, context.session, substituteTypes = false)
        if (supertypes.none { it.classId == PARCELABLE_ID }) {
            reporter.reportOn(source, KtErrorsParcelize.NO_PARCELABLE_SUPERTYPE, context)
        }

        val parcelableType = PARCELABLE_ID.constructClassLikeType()
        klass.delegateFieldsMap?.forEach { [index, _] ->
            val superTypeRef = klass.superTypeRefs[index]
            val superType = superTypeRef.coneType
            if (superType.isSubtypeOf(parcelableType, context.session)) {
                reporter.reportOn(superTypeRef.source, KtErrorsParcelize.PARCELABLE_DELEGATE_IS_NOT_ALLOWED, context)
            }
        }

        val constructorSymbols = klass.constructors(context.session)
        val primaryConstructorSymbol = constructorSymbols.find { it.isPrimary }
        val secondaryConstructorSymbols = constructorSymbols.filterNot { it.isPrimary }
        if (primaryConstructorSymbol == null && secondaryConstructorSymbols.isNotEmpty()) {
            reporter.reportOn(source, KtErrorsParcelize.PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR, context)
        }
    }

    private fun checkParcelerClass(klass: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (klass !is FirRegularClass || !klass.isCompanion) return
        if (klass.isSubclassOf(OLD_PARCELER_ID.toLookupTag(), context.session, isStrict = true)) {
            val strategy = if (klass.name == SpecialNames.NO_NAME_PROVIDED) {
                SourceElementPositioningStrategies.OBJECT_KEYWORD
            } else {
                SourceElementPositioningStrategies.NAME_IDENTIFIER
            }
            reporter.reportOn(klass.source, KtErrorsParcelize.DEPRECATED_PARCELER, context, positioningStrategy = strategy)
        }
    }
}

