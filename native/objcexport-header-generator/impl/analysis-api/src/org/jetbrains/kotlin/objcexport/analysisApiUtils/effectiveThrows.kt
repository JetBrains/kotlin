package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
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
context(KtAnalysisSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal val KtFunctionLikeSymbol.effectiveThrows: List<ClassId>
    get() {
        getAllOverriddenSymbols().firstOrNull()?.let { return (it as KtFunctionLikeSymbol).effectiveThrows }
        return definedThrows
    }