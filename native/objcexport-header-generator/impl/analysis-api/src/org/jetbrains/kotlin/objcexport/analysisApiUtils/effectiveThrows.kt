/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Returns root [Throws] classIds
 *
 * ```kotlin
 * class A {
 *   @Throws(ExcA)
 *   foo()
 * }
 *
 * class B: A {
 *   @Throws(ExcB)
 *   override foo()
 * }
 *
 * class C: B {
 *   @Throws(ExcC)
 *   override foo()
 * }
 *
 * C.foo.effectiveThrows = [ExcA]
 * ```
 *
 * See [definedThrows]
 * See K1: [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.getEffectiveThrows]
 */
internal fun KaSession.getEffectiveThrows(symbol: KaFunctionSymbol): List<ClassId> {
    symbol.allOverriddenSymbols.firstOrNull()?.let { return getEffectiveThrows((it as KaFunctionSymbol)) }
    return getDefinedThrows(symbol)
}
