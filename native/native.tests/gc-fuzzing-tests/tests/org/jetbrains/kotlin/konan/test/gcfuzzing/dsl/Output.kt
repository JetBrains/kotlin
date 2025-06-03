/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.dsl

enum class FileKind {
    KOTLIN,
    DEF,
    HEADER,
    OBJC_SOURCE,
}

class File(
    val filename: String,
    val kind: FileKind,
    val contents: String,
)

typealias Output = List<File>