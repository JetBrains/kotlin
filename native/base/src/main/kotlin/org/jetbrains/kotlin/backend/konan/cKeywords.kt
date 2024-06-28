/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

@InternalKotlinNativeApi
val cKeywords = setOf(
    // Actual C keywords.
    "auto", "break", "case",
    "char", "const", "continue",
    "default", "do", "double",
    "else", "enum", "extern",
    "float", "for", "goto",
    "if", "int", "long",
    "register", "return",
    "short", "signed", "sizeof", "static", "struct", "switch",
    "typedef", "union", "unsigned",
    "void", "volatile", "while",
    // C99-specific.
    "_Bool", "_Complex", "_Imaginary", "inline", "restrict",
    // C11-specific.
    "_Alignas", "_Alignof", "_Atomic", "_Generic", "_Noreturn", "_Static_assert", "_Thread_local",
    // Not exactly keywords, but reserved or standard-defined.
    "and", "not", "or", "xor",
    "bool", "complex", "imaginary",

    // C++ keywords not listed above.
    "alignas", "alignof", "and_eq", "asm",
    "bitand", "bitor", "bool",
    "catch", "char16_t", "char32_t", "class", "compl", "constexpr", "const_cast",
    "decltype", "delete", "dynamic_cast",
    "explicit", "export",
    "false", "friend",
    "inline",
    "mutable",
    "namespace", "new", "noexcept", "not_eq", "nullptr",
    "operator", "or_eq",
    "private", "protected", "public",
    "reinterpret_cast",
    "static_assert",
    "template", "this", "thread_local", "throw", "true", "try", "typeid", "typename",
    "using",
    "virtual",
    "wchar_t",
    "xor_eq"
)
