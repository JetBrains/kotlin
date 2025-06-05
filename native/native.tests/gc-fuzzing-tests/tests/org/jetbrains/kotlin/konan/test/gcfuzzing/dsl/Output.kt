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

val Output.kotlinFilename: String
    get() = find { it.kind == FileKind.KOTLIN }!!.filename

val Output.kotlinFrameworkName: String
    get() = "KotlinObjCFramework"

val Output.kotlinFrameworkArgs: List<String>
    get() = listOf("-Xstatic-framework", "-Xbinary=bundleId=${kotlinFrameworkName}")

val Output.defFilename: String
    get() = find { it.kind == FileKind.DEF }!!.filename

val Output.cinteropArgs: List<String>
    get() = emptyList()

val Output.headerFilename: String
    get() = find { it.kind == FileKind.HEADER }!!.filename

val Output.objcSourceFilename: String
    get() = find { it.kind == FileKind.OBJC_SOURCE }!!.filename

val Output.objcSourceArgs: List<String>
    get() = listOf("-fobjc-arc")

fun Output.save(root: java.io.File) {
    root.mkdirs()
    forEach {
        root.resolve(it.filename).writeText(it.contents)
    }
}