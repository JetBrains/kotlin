/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

/**
 * The common base class of all enum classes.
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/enum-classes.html) for more
 * information on enum classes.
 */
public abstract class Enum<E: Enum<E>>(public val name: String, public val ordinal: Int): Comparable<E> {

    public companion object {
    }

    public override final fun compareTo(other: E): Int { return ordinal - other.ordinal }

    public override final fun equals(other: Any?): Boolean {
        return this === other
    }

    public override final fun hashCode(): Int {
        return ordinal
    }

    public override fun toString(): String {
        return name
    }
}

@Suppress("UNUSED_PARAMETER")
public fun <T: Enum<T>> enumValueOf(name: String): T {
    throw Exception("Call to this function should've been lowered")
}

public fun <T: Enum<T>> enumValues(): Array<T> {
    throw Exception("Call to this function should've been lowered")
}
