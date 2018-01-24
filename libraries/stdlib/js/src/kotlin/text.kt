/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.text


public actual interface Appendable {
    public actual fun append(csq: CharSequence?): Appendable
    public actual fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    public actual fun append(c: Char): Appendable
}

public actual class StringBuilder(content: String) : Appendable, CharSequence {
    actual constructor(@Suppress("UNUSED_PARAMETER") capacity: Int) : this() {}

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
