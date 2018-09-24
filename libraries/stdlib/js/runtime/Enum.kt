/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

public class Enum<T : Enum<T>> : Comparable<Enum<T>> {
    @JsName("name$") private var _name: String = ""
    @JsName("ordinal$") private var _ordinal: Int = 0

    val name: String
        get() = _name

    val ordinal: Int
        get() = _ordinal

    override fun compareTo(other: Enum<T>) = ordinal.compareTo(other.ordinal)

    override fun equals(other: Any?) = this === other

    override fun hashCode(): Int = js("Kotlin.identityHashCode")(this)

    override fun toString() = name

    companion object
}