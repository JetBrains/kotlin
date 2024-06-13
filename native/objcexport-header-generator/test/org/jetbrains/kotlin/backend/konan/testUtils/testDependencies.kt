/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString

val testDependencyKlibs = System.getProperty("testDependencyKlibs").orEmpty()
    .split(File.pathSeparator)
    .map(::Path)

val testLibraryAKlibFile
    get() = testDependencyKlibs.firstOrNull { it.contains(Path("testLibraryA")) }
        ?: error("Missing 'testLibraryA' in 'testDependencyKlibs' System Property")

val testLibraryBKlibFile
    get() = testDependencyKlibs.firstOrNull { it.contains(Path("testLibraryB")) }
        ?: error("Missing 'testLibraryB' in 'testDependencyKlibs' System Property")


val testLibraryKotlinxSerializationJson
    get() = testDependencyKlibs.firstOrNull {
        it.pathString.contains("serialization-json")
    } ?: error("Missing 'kotlinx-serialization-json' in 'testDependencyKlibs' System Property")

val testLibraryKotlinxSerializationCore
    get() = testDependencyKlibs.firstOrNull {
        it.pathString.contains("serialization-core")
    } ?: error("Missing 'kotlinx-serialization-core' in 'testDependencyKlibs' System Property")

val testLibraryKotlinxDatetime
    get() = testDependencyKlibs.firstOrNull {
        it.pathString.contains("datetime")
    } ?: error("Missing 'kotlinx-datetime' in 'testDependencyKlibs' System Property")

val testLibraryKotlinxCoroutines
    get() = testDependencyKlibs.firstOrNull {
        it.pathString.contains("coroutines")
    } ?: error("Missing 'kotlinx-coroutines' in 'testDependencyKlibs' System Property")