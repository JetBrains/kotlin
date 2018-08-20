/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.text


public actual interface Appendable {
    public actual fun append(csq: CharSequence?): Appendable
    public actual fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    public actual fun append(c: Char): Appendable
}

public actual class StringBuilder(content: String = "") : Appendable, CharSequence {
    actual constructor(capacity: Int) : this() {}

    actual constructor(content: CharSequence) : this(content.toString()) {}

    actual constructor() : this("")

    private var string: String = content

    actual override val length: Int
        get() = string.asDynamic().length

    actual override fun get(index: Int): Char = string[index]

    actual override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.substring(startIndex, endIndex)

    actual override fun append(c: Char): StringBuilder {
        string += c
        return this
    }

    actual override fun append(csq: CharSequence?): StringBuilder {
        string += csq.toString()
        return this
    }

    actual override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder {
        string += csq.toString().substring(start, end)
        return this
    }

    actual fun append(obj: Any?): StringBuilder {
        string += obj.toString()
        return this
    }

    actual fun reverse(): StringBuilder {
        string = string.asDynamic().split("").reverse().join("")
        return this
    }

    override fun toString(): String = string
}
