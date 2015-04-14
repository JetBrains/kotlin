/*
 * Copyright 2010-2015 JetBrains s.r.o.
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


native public class RegExp(pattern: String, flags: String? = null) {

    public fun test(str: String): Boolean = noImpl

    public fun exec(str: String): Array<String?>? = noImpl

    public override fun toString(): String = noImpl

    /**
     * The lastIndex is a read/write integer property of regular expressions that specifies the index at which to start the next match.
     */
    public var lastIndex: Int

    public val global: Boolean get() = noImpl
    public val ignoreCase: Boolean get() = noImpl
    public val multiline: Boolean get() = noImpl
}

public fun RegExp.reset() {
    lastIndex = 0
}


native public trait RegExpMatch {
    public val index: Int
    public val input: String
}
