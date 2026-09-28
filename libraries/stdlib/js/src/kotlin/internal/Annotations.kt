/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

/**
 * The effects that a function produces.
 * There are two kinds, not including pure functions: read-only and read-write.
 * The former only reads the state of the outside world, the calls to these functions can be reordered.
 * The latter mutates the state, so non-pure function calls cannot be moved across calls of this function.
 */
internal enum class EffectsKind {
    /** No effects, the function is pure. */
    PURE,

    /** The function can read from the outside world state. */
    READ,

    /** The function can both read from and write to the outside world state. This "includes" [READ]. */
    WRITE,
}

/**
 * Specifies the effects that this function has.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
internal annotation class Effects(val kind: EffectsKind)
