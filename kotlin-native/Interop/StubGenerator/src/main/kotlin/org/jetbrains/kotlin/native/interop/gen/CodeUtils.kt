/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.native.interop.gen

val kotlinKeywords = setOf(
        "as", "break", "class", "continue", "do", "dynamic", "else", "false", "for", "fun", "if", "in",
        "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
        "true", "try", "typealias", "val", "var", "when", "while",
        // While not technically keywords, those shall be escaped as well.
        "_", "__", "___"
)

/**
 * The expression written in native language.
 */
typealias NativeExpression = String

/**
 * The expression written in Kotlin.
 */
typealias KotlinExpression = String

/**
 * For this identifier constructs the string to be parsed by Kotlin as `SimpleName`
 * defined [here](https://kotlinlang.org/docs/reference/grammar.html#SimpleName).
 */
fun String.asSimpleName(): String = if (this in kotlinKeywords || this.contains("$")) {
    "`$this`"
} else {
    this
}

/**
 * Yet another mangler, particularly to avoid secondary clash, e.g. when a property
 * in prototype (interface) is mangled and that will cause another clash in the class
 * which implements this interface.
 * Rationale: keep algorithm simple but use the mangling characters which are rare
 * in normal code, and keep mangling easy readable.
 */
internal fun mangleSimple(name: String): String {
    val reserved = setOf("Companion")
    val postfix = "\$"
    return if (name in reserved)
        "$name$postfix"
    else
        name
}

/**
 * Returns the expression to be parsed by Kotlin as string literal with given contents,
 * i.e. transforms `foo$bar` to `"foo\$bar"`.
 */
fun String.quoteAsKotlinLiteral(): KotlinExpression = buildString {
    append('"')

    this@quoteAsKotlinLiteral.forEach { c ->
        when (c) {
            in charactersAllowedInKotlinStringLiterals -> append(c)
            '$' -> append("\\$")
            else -> append("\\u" + "%04X".format(c.code))
        }
    }

    append('"')
}

// TODO: improve literal readability by preserving more characters.

private val charactersAllowedInKotlinStringLiterals: Set<Char> = mutableSetOf<Char>().apply {
    addAll('a' .. 'z')
    addAll('A' .. 'Z')
    addAll('0' .. '9')
    addAll(listOf('_', '@', ':', ';', '.', ',', '{', '}', '=', '[', ']', '^', '#', '*', ' ', '(', ')'))
}

val annotationForUnableToImport
    get() = "@Deprecated(${"Unable to import this declaration".quoteAsKotlinLiteral()}, level = DeprecationLevel.ERROR)"
