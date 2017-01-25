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

package kotlin.text.js


public external class RegExp(pattern: String, flags: String? = definedExternally) {

    public fun test(str: String): Boolean

    public fun exec(str: String): RegExpMatch?

    public override fun toString(): String

    /**
     * The lastIndex is a read/write integer property of regular expressions that specifies the index at which to start the next match.
     */
    public var lastIndex: Int

    public val global: Boolean
    public val ignoreCase: Boolean
    public val multiline: Boolean
}

public fun RegExp.reset() {
    lastIndex = 0
}

// TODO: Inherit from array or introduce asArray() extension
public external interface RegExpMatch {
    public val index: Int
    public val input: String
    public val length: Int
}

public inline operator fun RegExpMatch.get(index: Int): String? = asDynamic()[index]

public inline fun RegExpMatch.asArray(): Array<out String?> = unsafeCast<Array<out String?>>()