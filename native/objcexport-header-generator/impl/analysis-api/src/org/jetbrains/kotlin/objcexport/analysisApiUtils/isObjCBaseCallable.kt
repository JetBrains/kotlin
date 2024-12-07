/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol

/**
 * Check that given [descriptor] is a so-called "base method", i.e. method
 * that doesn't override anything in a generated Objective-C interface.
 * Note that it does not mean that it has no "override" keyword.
 * Consider example:
 * ```kotlin
 * private interface I {
 *     fun f()
 * }
 *
 * class C : I {
 *     override fun f() {}
 * }
 * ```
 * Interface `I` is not exposed to the generated header, so C#f is considered to be a base method even though it has an "override" keyword.
 */
fun KaSession.isObjCBaseCallable(symbol: KaCallableSymbol): Boolean {
    return symbol.allOverriddenSymbols.none { overriddenSymbol ->
        isVisibleInObjC(overriddenSymbol)
    }
}
