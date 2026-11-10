/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirResolvedQualifierChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.parcelize.fir.diagnostics.*

class FirParcelizeCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val annotationCallCheckers: Set<FirAnnotationCallChecker> = setOf(FirParcelizeAnnotationChecker)
        override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker> = setOf(FirParcelizeInternalParcelableQualifierChecker)
    }

    override val typeCheckers: TypeCheckers = object : TypeCheckers() {
        override val resolvedTypeRefCheckers: Set<FirResolvedTypeRefChecker> = setOf(FirParcelizeInternalParcelableTypeChecker)
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirClassChecker> = setOf(FirParcelizeClassChecker, FirPolymorphicSealedClassChecker)
        override val propertyCheckers: Set<FirPropertyChecker> = setOf(FirParcelizePropertyChecker)
        override val namedFunctionCheckers: Set<FirNamedFunctionChecker> = setOf(FirParcelizeFunctionChecker)
        override val constructorCheckers: Set<FirConstructorChecker> = setOf(FirParcelizeConstructorChecker)
    }
}
