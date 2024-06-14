package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
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
context(KaSession)
internal val KaFunctionLikeSymbol.effectiveThrows: List<ClassId>
    get() {
        allOverriddenSymbols.firstOrNull()?.let { return (it as KaFunctionLikeSymbol).effectiveThrows }
        return definedThrows
    }