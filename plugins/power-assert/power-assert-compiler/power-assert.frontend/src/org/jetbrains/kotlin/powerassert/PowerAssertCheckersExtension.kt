/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.powerassert.checkers.PowerAssertExplanationAccessChecker
import org.jetbrains.kotlin.powerassert.checkers.PowerAssertRuntimeChecker

class PowerAssertCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker>
            get() = setOf(PowerAssertRuntimeChecker)

        override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker>
            get() = setOf(PowerAssertExplanationAccessChecker)
    }
}
