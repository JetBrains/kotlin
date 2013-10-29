/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast


public open class Identifier(val name: String,
                             val myNullable: Boolean = true,
                             val quotingNeeded: Boolean = true) : Expression() {
    public override fun isEmpty() = name.length() == 0

    private open fun ifNeedQuote(): String {
        if (quotingNeeded && (ONLY_KOTLIN_KEYWORDS.contains(name)) || name.contains("$")) {
            return quote(name)
        }

        return name
    }

    public override fun toKotlin(): String = ifNeedQuote()
    public override fun isNullable(): Boolean = myNullable

    class object {
        public val EMPTY_IDENTIFIER: Identifier = Identifier("")
        private open fun quote(str: String): String {
            return "`" + str + "`"
        }

        public val ONLY_KOTLIN_KEYWORDS: Set<String> = hashSet(
                "package", "as", "type", "val", "var", "fun", "is", "in", "object", "when", "trait", "This"
        );
    }
}
