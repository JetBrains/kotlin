/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package com.google.dart.compiler.backend.js.ast

import java.util.Stack

public fun JsObjectScope(parent: JsScope, description: String): JsObjectScope = JsObjectScope(parent, description, null)

public class JsObjectScope(parent: JsScope, description: String, scopeId: String?) : JsScope(parent, description, scopeId)

public object JsDynamicScope : JsScope(null, "Scope for dynamic declarations", null) {
    override fun doCreateName(name: String) = JsName(this, name)
}

public class JsFunctionScope(parent: JsScope, description: String) : JsScope(parent, description, null) {

    private val labelScopes = Stack<LabelScope>()
    private val topLabelScope: LabelScope?
        get() = if (labelScopes.isNotEmpty()) labelScopes.peek() else null

    override fun declareName(identifier: String): JsName = super.declareFreshName(identifier)

    override fun hasOwnName(name: String): Boolean = RESERVED_WORDS.contains(name) || super.hasOwnName(name)

    public fun declareNameUnsafe(identifier: String): JsName = super.declareName(identifier)

    public fun enterLabel(label: String): JsName {
        val scope = LabelScope(topLabelScope, label)
        labelScopes.push(scope)
        return scope.labelName
    }

    public fun exitLabel() {
        assert(labelScopes.isNotEmpty()) { "No scope to exit from" }
        labelScopes.pop()
    }

    public fun findLabel(label: String): JsName? =
            topLabelScope?.findName(label)

    private inner class LabelScope(parent: LabelScope?, val ident: String) : JsScope(parent, "Label scope for $ident", null) {
        val labelName: JsName

        {
            val freshIdent = when {
                ident in RESERVED_WORDS -> getFreshIdent(ident)
                parent != null -> parent.getFreshIdent(ident)
                else -> ident
            }

            labelName = JsName(this@JsFunctionScope, freshIdent)
        }

        override fun findOwnName(name: String): JsName? =
                if (name == ident) labelName else null

        /**
         * Safe call is necessary, because hasOwnName can be called
         * in constructor before labelName is initialized (see KT-4394)
         */
        [suppress("UNNECESSARY_SAFE_CALL")]
        override fun hasOwnName(name: String): Boolean =
                name in RESERVED_WORDS
                || name == ident
                || name == labelName?.getIdent()
                || getParent()?.hasOwnName(name) ?: false
    }

    default object {
        public val RESERVED_WORDS: Set<String> = setOf(
                // keywords
                "await", "break", "case", "catch", "continue", "debugger", "default", "delete", "do", "else", "finally", "for", "function", "if",
                "in", "instanceof", "new", "return", "switch", "this", "throw", "try", "typeof", "var", "void", "while", "with",

                // future reserved words
                "class", "const", "enum", "export", "extends", "import", "super",

                // as future reserved words in strict mode
                "implements", "interface", "let", "package", "private", "protected", "public", "static", "yield",

                // additional reserved words
                "null", "true", "false",

                // disallowed as variable names in strict mode
                "eval", "arguments",

                // non-reserved words that act like reserved words
                "NaN", "Infinity", "undefined",

                // the special Kotlin object
                "Kotlin"
        )
    }
}
