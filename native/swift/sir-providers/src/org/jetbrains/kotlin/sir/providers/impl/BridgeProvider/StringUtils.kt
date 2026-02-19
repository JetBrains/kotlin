/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.BridgeProvider

private const val KOTLIN_BASIC_IDENTIFIER_HEADER = """_\p{Lu}\p{Ll}\p{Lt}\p{Lm}\p{Lo}"""
private const val KOTLIN_BASIC_IDENTIFIER_BODY = """\p{Nd}$KOTLIN_BASIC_IDENTIFIER_HEADER"""

private val kotlinQuotedIdentifierRegex =
    """^[^\r\n`]+\$""".toRegex()
private val kotlinQuotedIdentifierNonCompliantRegex =
    """[\r\n`]+""".toRegex()

private val kotlinBasicIdentifierRegex =
    "^[$KOTLIN_BASIC_IDENTIFIER_HEADER][$KOTLIN_BASIC_IDENTIFIER_BODY]*\$".toRegex()

private val cIdentifierRegex =
    "^[_a-zA-Z][_a-zA-Z0-9]*\$".toRegex()
private val cIdentifierNonCompliantRegex =
    """^[^_a-zA-Z][^_a-zA-Z0-9]*+|(?<!^)[^_a-zA-Z0-9]+""".toRegex()

internal val String.cIdentifier: String
    get() = let {
        this.takeIf(cIdentifierRegex::matches) ?: this.replace(cIdentifierNonCompliantRegex) { match ->
            match.value.map {
                String.format("%02X", it.code)
            }.joinToString(separator = "", prefix = "U")
        }
    }.let { it.takeIf { !cKeywords.contains(it) } ?: "${it}_" }

private val cKeywords = setOf(
    "alignas", "alignof", "auto", "bool", "break", "case", "char", "const", "constexpr", "continue", "default", "do", "double", "else",
    "enum", "extern", "false", "float", "for", "goto", "id", "if", "inline", "int", "long", "nullptr", "register", "restrict", "return", "short",
    "signed", "sizeof", "static", "static_assert", "struct", "switch", "thread_local", "true", "typedef", "typeof", "typeof_unqual",
    "union", "unsigned", "void", "volatile", "while", "_Alignas", "_Alignof", "_Atomic", "_BitInt", "_Bool", "_Complex", "_Decimal128",
    "_Decimal32", "_Decimal64", "_Generic", "_Imaginary", "_Noreturn", "_Static_assert", "_Thread_local"
)

internal val String.kotlinIdentifier: String
    get() = this.takeIf { kotlinBasicIdentifierRegex.matches(it) && !kotlinKeywords.contains(it) && it.any { it != '_' } }
        ?: (this.takeIf(kotlinQuotedIdentifierRegex::matches)
            ?: this.replace(kotlinQuotedIdentifierNonCompliantRegex) { "" }).let { "`$it`" }

private val kotlinKeywords = setOf(
    "return@", "continue@", "break@", "this@", "super@", "file", "field", "property", "get", "set", "receiver", "param", "setparam",
    "delegate", "package", "import", "class", "interface", "fun", "object", "val", "var", "typealias", "constructor", "by", "companion",
    "init", "this", "super", "typeof", "where", "if", "else", "when", "try", "catch", "finally", "for", "do", "while", "throw", "return",
    "continue", "break", "as", "is", "in", "!is", "!in", "out", "dynamic", "public", "private", "protected", "internal", "enum", "sealed",
    "annotation", "data", "inner", "tailrec", "operator", "inline", "infix", "external", "suspend", "override", "abstract", "final", "open",
    "const", "lateinit", "vararg", "noinline", "crossinline", "reified", "expect", "actual"
)

internal fun createBridgeParameterName(kotlinName: String): String {
    // TODO: Post-process because C has stricter naming conventions.
    return kotlinName
}
