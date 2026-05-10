/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString

/**
 * Models the real world libraries that are published like this to Maven:
 * one main "regular" Kotlin klib and any number of cinterop klibs.
 *
 * The IDE sometimes groups them together (e.g. in KaLibraryModule), so it is important to test like this.
 */
data class Library(
    val mainKlib: Path,
    val cinteropKlibs: List<Path> = emptyList(),
) {
    val klibs
        get() = listOf(mainKlib) + cinteropKlibs

    val name: String
        get() = mainKlib.nameWithoutExtension
}

private val testDependencyKlibs = System.getProperty("testDependencyKlibs").orEmpty()
    .split(File.pathSeparator)
    .map(::Path)

private fun getLibrary(name: String): Library {
    val (cinteropKlibs, regularKlibs) = testDependencyKlibs
        .filter { it.fileName.pathString.contains(name) }
        .partition { it.fileName.pathString.contains("interop") }

    return Library(
        mainKlib = regularKlibs.singleOrNull() ?: error("Missing '$name' in 'testDependencyKlibs' System Property"),
        cinteropKlibs = cinteropKlibs
    )
}

val testLibraryA: Library
    get() = getLibrary("testLibraryA")

val testLibraryAKlibFile
    get() = testLibraryA.mainKlib

val testLibraryB: Library
    get() = getLibrary("testLibraryB")

val testLibraryBKlibFile
    get() = testLibraryB.mainKlib

val testLibraryC: Library
    get() = getLibrary("testLibraryC")

val testInternalLibrary: Library
    get() = getLibrary("testInternalLibrary")

val testExtensionsLibrary: Library
    get() = getLibrary("testExtensionsLibrary")

val testLibraryKotlinxSerializationJson: Library
    get() = getLibrary("serialization-json")

val testLibraryKotlinxSerializationCore: Library
    get() = getLibrary("serialization-core")

val testLibraryKotlinxSerializationCoreKlibFile
    get() = testLibraryKotlinxSerializationCore.mainKlib

val testLibraryKotlinxDatetime: Library
    get() = getLibrary("datetime")

val testLibraryKotlinxCoroutines: Library
    get() = getLibrary("coroutines")

val testLibraryKotlinxCoroutinesKlibFile
    get() = testLibraryKotlinxCoroutines.mainKlib

val testLibraryAtomicFu: Library
    get() = getLibrary("atomicfu")

val testLibraryAtomicFuKlibFile
    get() = testLibraryAtomicFu.mainKlib

val testLibraryAtomicFuCinteropInteropKlibFile
    get() = testLibraryAtomicFu.cinteropKlibs.single()
