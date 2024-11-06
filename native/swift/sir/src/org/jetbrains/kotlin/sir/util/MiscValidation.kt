/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

// https://docs.swift.org/swift-book/documentation/the-swift-programming-language/lexicalstructure/#Identifiers
private val SWIFT_IDENTIFIER_HEADER = """
_a-zA-Z
\u00A8\u00AA\u00AD\u00AF\u00B2-\u00B5\u00B7-\u00BA
\u00BC-\u00BE\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u00FF
\u0100-\u02FF\u0370-\u167F\u1681-\u180D\u180F-\u1DBF
\u1E00-\u1FFF
\u200B-\u200D\u202A-\u202E\u203F-\u2040\u2054\u2060-\u206F
\u2070-\u20CF\u2100-\u218F\u2460-\u24FF\u2776-\u2793
\u2C00-\u2DFF\u2E80-\u2FFF
\u3004-\u3007\u3021-\u302F\u3031-\u303F\u3040-\uD7FF
\uF900-\uFD3D\uFD40-\uFDCF\uFDF0-\uFE1F\uFE30-\uFE44
\uFE47-\uFFFD
\x{10000}-\x{1FFFD}\x{20000}-\x{2FFFD}\x{30000}-\x{3FFFD}\x{40000}-\x{4FFFD}
\x{50000}-\x{5FFFD}\x{60000}-\x{6FFFD}\x{70000}-\x{7FFFD}\x{80000}-\x{8FFFD}
\x{90000}-\x{9FFFD}\x{A0000}-\x{AFFFD}\x{B0000}-\x{BFFFD}\x{C0000}-\x{CFFFD}
\x{D0000}-\x{DFFFD}\x{E0000}-\x{EFFFD}
""".replace("\n", "")

private val SWIFT_IDENTIFIER_BODY = """
$SWIFT_IDENTIFIER_HEADER
0-9
\u0300-\u036F\u1DC0-\u1DFF\u20D0-\u20FF\uFE20\uFE2F
""".replace("\n", "")

private val swiftIdentifierRegex =
    Regex("^[$SWIFT_IDENTIFIER_HEADER][$SWIFT_IDENTIFIER_BODY]*\$")

private val swiftIdentifierNonCompliantRegex =
    Regex("^[^$SWIFT_IDENTIFIER_HEADER][^$SWIFT_IDENTIFIER_BODY]*|(?<!^)[^$SWIFT_IDENTIFIER_BODY]+")

// https://docs.swift.org/swift-book/documentation/the-swift-programming-language/lexicalstructure/
private val swiftKeywords = setOf(
    // Declarations
    "associatedtype", "borrowing", "class", "consuming", "deinit", "enum", "extension", "fileprivate", "func", "import", "init", "inout",
    "internal", "let", "nonisolated", "open", "operator", "private", "precedencegroup", "protocol", "public", "rethrows", "static",
    "struct", "subscript", "typealias", "var",

    // Statements
    "break", "case", "catch", "continue", "default", "defer", "do", "else", "fallthrough", "for", "guard", "if", "in", "repeat", "return",
    "throw", "switch", "where", "while",

    // Expressions and types
    "Any", "as", "await", "catch", "false", "is", "nil", "rethrows", "self", "Self", "super", "throw", "throws", "true", "try",
)

public val String.swiftSanitizedName: String
    get() = this.takeIf(swiftIdentifierRegex::matches) ?: this.replace(swiftIdentifierNonCompliantRegex) { match ->
        match.value.map {
            String.format("%02x", it.code)
        }.joinToString(
            separator = "",
            prefix = "_u".takeIf { match.range.first != 0 } ?: "u",
            postfix = "_".takeIf { match.range.last + 1 != this.length } ?: "",
        )
    }

public val String.isValidSwiftIdentifier: Boolean
    get() = swiftIdentifierRegex.matches(this)

public val String.swiftIdentifier: String
    get() = swiftSanitizedName.let { if (swiftKeywords.contains(it)) "`$it`" else it }.ifEmpty { "_" }

public val String.swiftStringLiteral: String
    get() {
        val isMultiline = this.any { it == '\u000A' || it == '\u000D' }
        val isRaw = this.any { it == '\\' || it == '"' }
        val delimiter = if (isMultiline) "\"\"\"" else "\""
        val separator = if (isMultiline) "\n" else ""
        val boundary = if (isRaw) (1..<length).asSequence().map { "#".repeat(it) }.find { !this.contains("\"$it") } else ""
        return "$boundary$delimiter$separator$this$separator$delimiter$boundary"
    }