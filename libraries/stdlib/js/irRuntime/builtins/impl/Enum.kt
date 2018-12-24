/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.js.*

abstract class Enum<E : Enum<E>>(val name: String, val ordinal: Int) : Comparable<E> {

    override fun compareTo(other: E) = ordinal.compareTo(other.ordinal)

    override fun equals(other: Any?) = this === other

    override fun hashCode(): Int = identityHashCode(this)

    override fun toString() = name

    companion object
}

// Use non-inline calls to enumValuesIntrinsic and enumValueOfIntrinsic calls in order
// for compiler to replace them with method calls of concrete enum classes after inlining.
// TODO: Figure out better solution (Inline hacks? Dynamic calls to stable mangled names?)

@SinceKotlin("1.1")
public inline fun <reified T : Enum<T>> enumValues(): Array<T> = enumValuesIntrinsic<T>()

@SinceKotlin("1.1")
public inline fun <reified T : Enum<T>> enumValueOf(name: String): T = enumValueOfIntrinsic<T>(name)
