/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

// Per https://docs.swift.org/swift-book/documentation/the-swift-programming-language/lexicalstructure/#Identifiers
const val IDENTIFIER_SPECIAL_RANGES_HEAD = "\\u00A8\\u00AA\\u00AD\\u00AF\\u00B2–\\u00B5\\u00B7–\\u00BA\\u00BC–\\u00BE\\u00C0–\\u00D6\\u00D8–\\u00F6\\u00F8–\\u00FF\\u0100–\\u02FF\\u0370–\\u167F\\u1681–\\u180D\\u180F–\\u1DBF\\u1E00–\\u1FFF\\u200B–\\u200D\\u202A–\\u202E\\u203F–\\u2040\\u2054\\u2060–\\u206F\\u2070–\\u20CF\\u2100–\\u218F\\u2460–\\u24FF\\u2776–\\u2793\\u2C00–\\u2DFF\\u2E80–\\u2FFF\\u3004–\\u3007\\u3021–\\u302F\\u3031–\\u303F\\u3040–\\uD7FF\\uF900–\\uFD3D\\uFD40–\\uFDCF\\uFDF0–\\uFE1F\\uFE30–\\uFE44\\uFE47–\\uFFFD\\u10000–\\u1FFFD\\u20000–\\u2FFFD\\u30000–\\u3FFFD\\u40000–\\u4FFFD\\u50000–\\u5FFFD\\u60000–\\u6FFFD\\u70000–\\u7FFFD\\u80000–\\u8FFFD\\u90000–\\u9FFFD\\uA0000–\\uAFFFD\\uB0000–\\uBFFFD\\uC0000–\\uCFFFD\\uD0000–\\uDFFFD\\uE0000–\\uEFFFD"
const val IDENTIFIER_SPECIAL_RANGES_TAIL = "\\u0300–\\u036F\\u1DC0–\\u1DFF\\u20D0–\\u20FF\\uFE20–\\uFE2F$IDENTIFIER_SPECIAL_RANGES_HEAD"
val IDENTIFIER_REGEX = Regex("^[_a-zA-Z$IDENTIFIER_SPECIAL_RANGES_HEAD][_a-zA-Z0-9$IDENTIFIER_SPECIAL_RANGES_TAIL]*$")

public fun CharSequence.isValidSwiftIdentifier(): Boolean = IDENTIFIER_REGEX.matches(this)