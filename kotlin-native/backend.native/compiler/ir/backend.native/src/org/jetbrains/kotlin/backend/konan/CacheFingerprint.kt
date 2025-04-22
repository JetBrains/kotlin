/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.backend.common.serialization.SerializedIrFileFingerprint
import org.jetbrains.kotlin.backend.common.serialization.SerializedKlibFingerprint
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.javaFile
import org.jetbrains.kotlin.library.isNativeStdlib
import java.io.ByteArrayOutputStream
import java.io.File

private fun File.computeCacheFingerprint(): FingerprintHash {
    return SerializedKlibFingerprint(this).klibFingerprint
}

internal fun KotlinLibrary.computeCacheFingerprint(embeddedDependenciesHashes: List<FingerprintHash>): FingerprintHash {
    return (listOf(libraryFile.javaFile().computeCacheFingerprint()) + embeddedDependenciesHashes).fold()
}

internal fun KotlinLibrary.computeCacheFingerprintForIrFile(embeddedDependenciesHashes: List<FingerprintHash>, fileIndex: Int): FingerprintHash {
    return (listOf(SerializedIrFileFingerprint(this, fileIndex).fileFingerprint) + embeddedDependenciesHashes).fold()
}

internal fun List<FingerprintHash>.fold() = FingerprintHash(fold(Hash128Bits(size.toULong())) { acc, x -> acc.combineWith(x.hash) })

internal fun KotlinLibrary.embeddedDependenciesHashes(distribution: Distribution, runtimeNativeLibraries: List<String>): List<FingerprintHash> {
    val runtimeHashes = when {
        isNativeStdlib -> runtimeNativeLibraries.map { File(it).computeCacheFingerprint() }
        else -> emptyList()
    }
    val compilerBinaries = buildList {
        val distributionDirectory = File(distribution.konanHome)
        add(distributionDirectory.resolve("konan/lib/kotlin-native-compiler-embeddable.jar"))
        val nativeLibs = distributionDirectory.resolve("konan/nativelib")
        addAll(nativeLibs.listFiles())
    }
    val compilerHashes = compilerBinaries.map { it.computeCacheFingerprint() }
    val propertiesHash = ByteArrayOutputStream().use {
        distribution.properties.store(it, "")
        FingerprintHash.fromByteArray(it.toByteArray())
    }
    return listOf(propertiesHash) + compilerHashes + runtimeHashes
}