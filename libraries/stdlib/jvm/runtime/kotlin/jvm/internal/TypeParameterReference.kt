/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance
import kotlin.reflect.typeOf

@SinceKotlin("1.4")
public class TypeParameterReference(
    container: TypeParameterContainer,
    override val name: String,
    override val variance: KVariance,
    override val isReified: Boolean,
) : KTypeParameterBase(container) {
    @Volatile
    private var bounds: List<KType>? = null

    @OptIn(ExperimentalStdlibApi::class)
    override val upperBounds: List<KType>
        get() = bounds ?: listOf(typeOf<Any?>()).also { bounds = it }

    public fun setUpperBounds(upperBounds: List<KType>) {
        // This assertion is only checking that the typeOf compiler implementation didn't generate some nonsense in bytecode.
        // Since this class is not used anywhere else, we don't use any locks to prevent double initialization here intentionally.
        if (bounds != null) {
            error("Upper bounds of type parameter '$this' have already been initialized.")
        }
        bounds = upperBounds
    }

    public companion object {
        public fun toString(typeParameter: KTypeParameter): String =
            buildString {
                when (typeParameter.variance) {
                    KVariance.INVARIANT -> {
                    }
                    KVariance.IN -> append("in ")
                    KVariance.OUT -> append("out ")
                }

                append(typeParameter.name)
            }
    }
}
