/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.js.*

@OptIn(ExperimentalStdlibApi::class)
@Suppress("DEPRECATION")
@EagerInitialization
internal val ENUM_HASHCODE_RANDOMIZER: Int = compileTimeRandomNumber(from = 0, to = Int.MAX_VALUE / 2)

public actual abstract class Enum<E : Enum<E>> actual constructor(@kotlin.internal.IntrinsicConstEvaluation public actual val name: String, public actual val ordinal: Int) : Comparable<E> {

    actual final override fun compareTo(other: E): Int = ordinal.compareTo(other.ordinal)

    actual final override fun equals(other: Any?): Boolean = this === other

    actual final override fun hashCode(): Int = ENUM_HASHCODE_RANDOMIZER + ordinal

    actual override fun toString(): String = name

    public actual companion object
}
