/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.Builtin

package kotlin.reflect

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic



/**
 * Represents a type projection. Type projection is usually the argument to another type in a type usage.
 * For example, in the type `Array<out Number>`, `out Number` is the covariant projection of the type represented by the class `Number`.
 *
 * Type projection is either the star projection, or an entity consisting of a specific type plus optional variance.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/generics.html#type-projections)
 * for more information.
 */
@SinceKotlin("1.1")
public data class KTypeProjection constructor(
    /**
     * The use-site variance specified in the projection, or `null` if this is a star projection.
     */
    public val variance: KVariance?,
    /**
     * The type specified in the projection, or `null` if this is a star projection.
     */
    public val type: KType?
) {

    init {
        require((variance == null) == (type == null)) {
            if (variance == null)
                "Star projection must have no type specified."
            else
                "The projection variance $variance requires type to be specified."
        }
    }

    override fun toString(): String = when (variance) {
        null -> "*"
        KVariance.INVARIANT -> type.toString()
        KVariance.IN -> "in $type"
        KVariance.OUT -> "out $type"
    }

    public companion object {
        // provided for compiler access
        @JvmField
        @PublishedApi
        internal val star: KTypeProjection = KTypeProjection(null, null)

        /**
         * Star projection, denoted by the `*` character.
         * For example, in the type `KClass<*>`, `*` is the star projection.
         * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/generics.html#star-projections)
         * for more information.
         */
        public val STAR: KTypeProjection get() = star

        /**
         * Creates an invariant projection of a given type. Invariant projection is just the type itself,
         * without any use-site variance modifiers applied to it.
         * For example, in the type `Set<String>`, `String` is an invariant projection of the type represented by the class `String`.
         */
        @JvmStatic
        public fun invariant(type: KType): KTypeProjection =
            KTypeProjection(KVariance.INVARIANT, type)

        /**
         * Creates a contravariant projection of a given type, denoted by the `in` modifier applied to a type.
         * For example, in the type `MutableList<in Number>`, `in Number` is a contravariant projection of the type of class `Number`.
         */
        @JvmStatic
        public fun contravariant(type: KType): KTypeProjection =
            KTypeProjection(KVariance.IN, type)

        /**
         * Creates a covariant projection of a given type, denoted by the `out` modifier applied to a type.
         * For example, in the type `Array<out Number>`, `out Number` is a covariant projection of the type of class `Number`.
         */
        @JvmStatic
        public fun covariant(type: KType): KTypeProjection =
            KTypeProjection(KVariance.OUT, type)
    }
}