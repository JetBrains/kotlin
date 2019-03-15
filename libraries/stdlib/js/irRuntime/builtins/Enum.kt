/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
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