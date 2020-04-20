/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.klib

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil.createConcurrentWeakValueMap
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.library.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.library.KLIB_MODULE_METADATA_FILE_NAME
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.library.readKonanLibraryVersioning
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import java.io.IOException
import java.util.*

// BUNCH: 192
abstract class KlibLoadingMetadataCacheCompat {

    // Use special CacheKey class instead of VirtualFile for cache keys. Certain types of VirtualFiles (for example, obtained from JarFileSystem)
    // do not compare path (url) and modification stamp in equals() method.
    private data class CacheKey(
        val url: String,
        val modificationStamp: Long
    ) {
        constructor(virtualFile: VirtualFile) : this(virtualFile.url, virtualFile.modificationStamp)
    }

    // ConcurrentWeakValueHashMap does not allow null values.
    private class CacheValue<T : Any>(val value: T?)

    private val packageFragmentCache = createConcurrentWeakValueMap<CacheKey, CacheValue<ProtoBuf.PackageFragment>>()
    private val moduleHeaderCache = createConcurrentWeakValueMap<CacheKey, CacheValue<KlibMetadataProtoBuf.Header>>()
    private val libraryMetadataVersionCache = createConcurrentWeakValueMap<CacheKey, CacheValue<KlibMetadataVersion>>()

    fun getCachedPackageFragment(packageFragmentFile: VirtualFile): ProtoBuf.PackageFragment? {
        check(packageFragmentFile.extension == KLIB_METADATA_FILE_EXTENSION) {
            "Not a package metadata file: $packageFragmentFile"
        }

        return packageFragmentCache.computeIfAbsent(
            CacheKey(packageFragmentFile)
        ) {
            CacheValue(computePackageFragment(packageFragmentFile))
        }.value
    }

    fun getCachedModuleHeader(moduleHeaderFile: VirtualFile): KlibMetadataProtoBuf.Header? {
        check(moduleHeaderFile.name == KLIB_MODULE_METADATA_FILE_NAME) {
            "Not a module header file: $moduleHeaderFile"
        }

        return moduleHeaderCache.computeIfAbsent(
            CacheKey(moduleHeaderFile)
        ) {
            CacheValue(computeModuleHeader(moduleHeaderFile))
        }.value
    }

    private fun isMetadataCompatible(libraryRoot: VirtualFile): Boolean {
        val manifestFile = libraryRoot.findChild(KLIB_MANIFEST_FILE_NAME) ?: return false

        val metadataVersion = libraryMetadataVersionCache.computeIfAbsent(
            CacheKey(manifestFile)
        ) {
            CacheValue(computeLibraryMetadataVersion(manifestFile))
        }.value ?: return false

        return metadataVersion.isCompatible()
    }

    private fun computePackageFragment(packageFragmentFile: VirtualFile): ProtoBuf.PackageFragment? {
        if (!isMetadataCompatible(packageFragmentFile.parent.parent.parent))
            return null

        return try {
            parsePackageFragment(packageFragmentFile.contentsToByteArray(false))
        } catch (_: IOException) {
            null
        }
    }

    private fun computeModuleHeader(moduleHeaderFile: VirtualFile): KlibMetadataProtoBuf.Header? {
        if (!isMetadataCompatible(moduleHeaderFile.parent.parent))
            return null

        return try {
            parseModuleHeader(moduleHeaderFile.contentsToByteArray(false))
        } catch (_: IOException) {
            null
        }
    }

    private fun computeLibraryMetadataVersion(manifestFile: VirtualFile): KlibMetadataVersion? = try {
        val versioning = Properties().apply { manifestFile.inputStream.use { load(it) } }.readKonanLibraryVersioning()
        versioning.metadataVersion?.let(BinaryVersion.Companion::parseVersionArray)?.let(::KlibMetadataVersion)
    } catch (_: IOException) {
        // ignore and cache null value
        null
    } catch (_: IllegalArgumentException) {
        // ignore and cache null value
        null
    }
}
