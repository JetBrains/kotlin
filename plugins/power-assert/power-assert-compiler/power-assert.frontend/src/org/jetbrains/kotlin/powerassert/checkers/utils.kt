/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.processOverriddenFunctionsWithActionSafe
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.powerassert.PowerAssertNames

context(context: CheckerContext)
fun FirBasedSymbol<*>.isPowerAssertFunction(): Boolean =
    (this as? FirFunctionSymbol)?.getBasePowerAssertAnnotated() != null

context(context: CheckerContext)
private fun FirFunctionSymbol<*>.getBasePowerAssertAnnotated(): FirFunctionSymbol<*>? {
    // It is possible to have an 'override' of a `@PowerAssert` annotated function in an 'expect' class.
    // But in that case, the overridden function is the source of truth.
    // Therefore, the 'isOverride' branch *must* be checked first.
    return when {
        isOverride -> {
            // Use of '@PowerAssert' on an override function is redundant.
            // To make sure a function is properly annotated, check the overridden functions.
            var found: FirFunctionSymbol<*>? = null
            (this as? FirNamedFunctionSymbol)?.processOverriddenFunctionsWithActionSafe {
                found = it.getBasePowerAssertAnnotated()
                if (found != null) ProcessorAction.STOP else ProcessorAction.NEXT
            }
            found
        }

        isActual -> {
            // Use of '@PowerAssert' is required on *both* the expect and actual functions.
            if (hasAnnotation(PowerAssertNames.POWER_ASSERT_CLASS_ID, context.session)) {
                getSingleMatchedExpectForActualOrNull()?.getBasePowerAssertAnnotated()
            } else {
                null
            }
        }

        hasAnnotation(PowerAssertNames.POWER_ASSERT_CLASS_ID, context.session) -> {
            // Function is annotated with '@PowerAssert'.
            this
        }

        else -> null
    }
}
