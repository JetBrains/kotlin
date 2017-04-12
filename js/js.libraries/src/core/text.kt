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


public interface Appendable {
    fun append(csq: CharSequence?): Appendable
    fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    fun append(c: Char): Appendable
}

public class StringBuilder(content: String = "") : Appendable, CharSequence {
    constructor(@Suppress("UNUSED_PARAMETER") capacity: Int) : this() {}

    constructor(content: CharSequence) : this(content.toString()) {}

    private var string: String = content

    override val length: Int
        get() = string.asDynamic().length

    override fun get(index: Int): Char = string[index]

    override fun subSequence(start: Int, end: Int): CharSequence = string.substring(start, end)

    override fun append(c: Char): StringBuilder {
        string += c
        return this
    }

    override fun append(csq: CharSequence?): StringBuilder {
        string += csq.toString()
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder {
        string += csq.toString().substring(start, end)
        return this
    }

    fun append(obj: Any?): StringBuilder {
        string += obj.toString()
        return this
    }

    fun reverse(): StringBuilder {
        string = string.asDynamic().split("").reverse().join("")
        return this
    }

    override fun toString(): String = string
}
