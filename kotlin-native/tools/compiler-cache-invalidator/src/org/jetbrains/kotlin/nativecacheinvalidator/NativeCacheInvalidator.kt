/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativecacheinvalidator

import org.jetbrains.kotlin.backend.konan.CachedLibraries
import org.jetbrains.kotlin.backend.konan.serialization.CacheMetadata
import org.jetbrains.kotlin.backend.konan.serialization.CacheMetadataSerializer
import org.jetbrains.kotlin.backend.konan.util.compilerFingerprint
import org.jetbrains.kotlin.backend.konan.util.runtimeFingerprint
import org.jetbrains.kotlin.backend.konan.util.systemCacheRootDirectory
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.logging.Logger

private class FingerprintedDistribution(private val distribution: Distribution) {
    val distributionRoot: File
        get() = File(distribution.konanHome)

    val systemCacheRoot: File
        get() = distribution.systemCacheRootDirectory

    val compilerFingerprint: String by lazy {
        distribution.compilerFingerprint
    }

    private val runtimeFingerprints = mutableMapOf<KonanTarget, String>()

    fun runtimeFingerprint(target: KonanTarget): String = runtimeFingerprints.computeIfAbsent(target) {
        distribution.runtimeFingerprint(target)
    }
}

private val Distribution.fingerprinted: FingerprintedDistribution
    get() = FingerprintedDistribution(this)

private class Cache(val rootDir: File, val metadata: CacheMetadata)

context(logger: Logger)
private fun FingerprintedDistribution.collectCaches(): List<Cache> = buildList {
    logger.info("Searching for compiler caches in $distributionRoot")
    val processedCount = systemCacheRoot.walk().onEnter { rootDir ->
        val metadataFile = rootDir.resolve(CachedLibraries.METADATA_FILE_NAME)
        if (metadataFile.exists()) {
            // This directory is a cache. Save it, and no need to traverse it any further
            logger.info("Found cache at ${rootDir.toRelativeString(distributionRoot)}")
            add(Cache(rootDir, metadataFile.bufferedReader().use {
                CacheMetadataSerializer.deserialize(it)
            }))
            false
        } else {
            true
        }
    }.count()
    logger.info("Found $size caches out of $processedCount file system entries")
}

context(logger: Logger)
private fun FingerprintedDistribution.validateStaleCache(cache: Cache): Boolean {
    if (cache.metadata.compilerFingerprint != compilerFingerprint) {
        logger.info("""
            |Cache at ${cache.rootDir.toRelativeString(distributionRoot)} is invalid. Compiler fingerprint mismatch.
            |    distribution: $compilerFingerprint
            |    cache:        ${cache.metadata.compilerFingerprint}
        """.trimMargin())
        return false
    }
    cache.metadata.runtimeFingerprint?.let { cacheRuntimeFingerprint ->
        val distributionRuntimeFingerprint = runtimeFingerprint(cache.metadata.target)
        if (cacheRuntimeFingerprint != distributionRuntimeFingerprint) {
            logger.info("""
            |Cache at ${cache.rootDir.toRelativeString(distributionRoot)} is invalid. Runtime fingerprint mismatch (for ${cache.metadata.target}).
            |    distribution: $distributionRuntimeFingerprint
            |    cache:        $cacheRuntimeFingerprint
        """.trimMargin())
            return false
        }
    }
    return true
}

context(logger: Logger)
fun Distribution.invalidateStaleCaches() = with(fingerprinted) {
    collectCaches().forEach {
        if (!validateStaleCache(it))
            it.rootDir.deleteRecursively()
    }
}