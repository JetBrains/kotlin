/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.text

private const val KOTLINX_ATOMICFU = "kotlinx.atomicfu"
private const val PUBLISHED_API = "kotlin.PublishedApi"

private fun FirProperty.isKotlinxAtomicfu(): Boolean = returnTypeRef.coneType.classId?.packageFqName?.asString() == KOTLINX_ATOMICFU

private fun FirProperty.isPublishedApi(): Boolean = annotations.any(::isMarkedWithPublishedApi)

private fun FirClassLikeSymbol<*>.isPublishedApi(): Boolean = resolvedAnnotationsWithClassIds.any(::isMarkedWithPublishedApi)
private fun FirClassLikeSymbol<*>.isPublic(): Boolean = resolvedStatus.visibility.isPublicAPI

private fun isMarkedWithPublishedApi(a: FirAnnotation): Boolean =
    a.annotationTypeRef.coneType.classId?.asFqNameString() == PUBLISHED_API

object AtomicfuPropertyChecker: FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isKotlinxAtomicfu()) return
        val containingClassSymbol = declaration.dispatchReceiverType?.toClassLikeSymbol(context.session)
        if (declaration.visibility.isPublicAPI &&
            (containingClassSymbol == null || containingClassSymbol.isPublic())) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.PUBLIC_ATOMICS_ARE_FORBIDDEN,
                declaration.source.text.toString(),
                context
            )
        } else {
            if ((declaration.visibility.isPublicAPI || declaration.isPublishedApi()) &&
                (containingClassSymbol == null || containingClassSymbol.isPublic() || containingClassSymbol.isPublishedApi())) {
                reporter.reportOn(
                    declaration.source,
                    AtomicfuErrors.PUBLISHED_API_ATOMICS_ARE_FORBIDDEN,
                    declaration.source.text.toString(),
                    context
                )
            }
        }
        if (declaration.isVar) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.ATOMIC_PROPERTIES_SHOULD_BE_VAL,
                declaration.source.text.toString(),
                context
            )
        }
    }
}