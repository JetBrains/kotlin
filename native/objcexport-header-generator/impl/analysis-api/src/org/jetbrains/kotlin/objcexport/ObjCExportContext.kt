package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol

/**
 * Solution for migration from context receivers to context parameters, until the latter aren't available
 */
data class ObjCExportContext(
    val analysisSession: KaSession,
    val exportSession: KtObjCExportSession,
    /**
     * We have two cases which it's important to see difference:
     * 1. Class has generic and it's used in callable
     * ```kotlin
     * class Foo<A: UpperBound>
     * ```
     * This parameter should be translated as generic
     * ```objective-c
     * @interface Foo
     *   initWithReturnA (A)
     * @end
     * ```
     * 2. Class has generic and its descendant uses it
     * ```kotlin
     * open class Foo<A: UpperBound>(val a: A)
     * class Bar : Foo<UpperBound1>
     * ```
     * `Bar` should be translated as upper bound
     * @interface Bar
     *   initWithReturnA (id<UpperBound>)
     * @end
     * ```
     *
     * To catch this difference we need to keep currently translated classifier.
     *
     * See more at [org.jetbrains.kotlin.objcexport.TranslateToObjCTypeKt.mapToReferenceTypeIgnoringNullability]
     */
    val classifierContext: KaClassSymbol? = null,
) {
    fun <T> withClassifierContext(symbol: KaClassSymbol, action: ObjCExportContext.() -> T): T {
        return copy(classifierContext = symbol).action()
    }
}