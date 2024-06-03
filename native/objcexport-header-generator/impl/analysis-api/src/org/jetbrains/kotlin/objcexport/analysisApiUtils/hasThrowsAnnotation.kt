package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.name.ClassId

/**
 * Returns if this callable has [Throws] annotation.
 * Super callables aren't included.
 *
 * ```kotlin
 * class A {
 *   @Throws
 *   fun foo()
 * }
 * class B: A() {
 *   override fun foo()
 * }
 *
 * A.foo.hasThrowsAnnotation = true
 * B.foo.hasThrowsAnnotation = false
 * ```
 */
internal val KtCallableSymbol.hasThrowsAnnotation: Boolean
    get() {
        return ClassId.topLevel(KonanFqNames.throws) in annotations
    }