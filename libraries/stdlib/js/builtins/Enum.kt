/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.js.*

abstract class Enum<E : Enum<E>>(@kotlin.internal.IntrinsicConstEvaluation val name: String, val ordinal: Int) : Comparable<E> {

    final override fun compareTo(other: E) = ordinal.compareTo(other.ordinal)

    final override fun equals(other: Any?) = this === other

    final override fun hashCode(): Int = identityHashCode(this)

    override fun toString() = name

    companion object
}