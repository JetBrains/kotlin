/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.serialization.CacheMetadata
import org.jetbrains.kotlin.backend.konan.serialization.CacheMetadataSerializer
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * From cache root extract and parse metadata.properties file.
 */
internal val File.cacheMetadataFile: CacheMetadata?
    get() {
        require(isDirectory) { "$this must be cache root" }
        val metadata = child(CachedLibraries.METADATA_FILE_NAME)
        if (!metadata.isFile)
            return null // Some caches may not have metadata.
        return CacheMetadataSerializer.deserialize(metadata.bufferedReader())
    }

internal sealed interface MetadataCheckResult {
    object Ok : MetadataCheckResult
    class Fail(val description: String) : MetadataCheckResult
}

internal fun File.checkMetadataFits(
        target: KonanTarget,
        compilerFingerprint: String,
        runtimeFingerprint: String?,
): MetadataCheckResult {
    val metadata = cacheMetadataFile ?: run {
        return MetadataCheckResult.Fail("no metadata file")
    }
    if (metadata.target != target) {
        return MetadataCheckResult.Fail("target mismatch: expected=$target actual=${metadata.target}")
    }
    if (metadata.compilerFingerprint != compilerFingerprint) {
        return MetadataCheckResult.Fail("compiler fingerprint mismatch: expected=$compilerFingerprint actual=${metadata.compilerFingerprint}")
    }
    metadata.runtimeFingerprint?.let { actualRuntimeFingerprint ->
        if (actualRuntimeFingerprint != runtimeFingerprint) {
            return MetadataCheckResult.Fail("$target target fingerprint mismatch: expected=$runtimeFingerprint actual=$actualRuntimeFingerprint")
        }
    }
    return MetadataCheckResult.Ok
}