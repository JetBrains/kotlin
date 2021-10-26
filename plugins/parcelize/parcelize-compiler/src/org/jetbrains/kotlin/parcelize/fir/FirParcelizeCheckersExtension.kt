/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.parcelize.fir.diagnostics.*

class FirParcelizeCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val key: FirPluginKey
        get() = FirParcelizePluginKey

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val annotationCallCheckers: Set<FirAnnotationCallChecker>
            get() = setOf(FirParcelizeAnnotationChecker)
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirClassChecker>
            get() = setOf(FirParcelizeClassChecker)

        override val propertyCheckers: Set<FirPropertyChecker>
            get() = setOf(FirParcelizePropertyChecker)

        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
            get() = setOf(FirParcelizeFunctionChecker)

        override val constructorCheckers: Set<FirConstructorChecker>
            get() = setOf(FirParcelizeConstructorChecker)
    }
}
