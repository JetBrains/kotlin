/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.backend.konan.llvm.KonanMetadata
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.library.KotlinLibrary

internal abstract class LlvmModuleSpecificationBase(protected val cachedLibraries: CachedLibraries) : LlvmModuleSpecification {
    override fun importsKotlinDeclarationsFromOtherObjectFiles(): Boolean =
            cachedLibraries.hasStaticCaches // A bit conservative but still valid.

    override fun importsKotlinDeclarationsFromOtherSharedLibraries(): Boolean =
            cachedLibraries.hasDynamicCaches // A bit conservative but still valid.

    override fun containsModule(module: IrModuleFragment): Boolean =
            containsModule(module.descriptor)

    override fun containsModule(module: ModuleDescriptor): Boolean =
            module.konanLibrary.let { it == null || containsLibrary(it) }

    override fun containsPackageFragment(packageFragment: IrPackageFragment): Boolean =
            packageFragment.konanLibrary.let { it == null || containsLibrary(it) }

    private val containsCache = mutableMapOf<IrDeclaration, Boolean>()

    // This is essentially memoizing the IrDeclaration.konanLibrary property -- so much of the implementation
    // is inlined here to take greater advantage of the cache.
    override fun containsDeclaration(declaration: IrDeclaration): Boolean = containsCache.getOrPut(declaration) {
        val metadata = ((declaration as? IrMetadataSourceOwner)?.metadata as? KonanMetadata)
        if (metadata != null) {
            (metadata.konanLibrary == null || containsLibrary(metadata.konanLibrary)) && declaration.getPackageFragment() !is IrExternalPackageFragment
        } else when (val parent = declaration.parent) {
            is IrPackageFragment -> parent.konanLibrary.let { it == null || containsLibrary(it) } && parent !is IrExternalPackageFragment
            is IrDeclaration -> containsDeclaration(parent)
            else -> TODO("Unexpected declaration parent: $parent")
        }
    }
}

internal class DefaultLlvmModuleSpecification(cachedLibraries: CachedLibraries)
    : LlvmModuleSpecificationBase(cachedLibraries) {
    override val isFinal = true

    override fun containsLibrary(library: KotlinLibrary): Boolean = !cachedLibraries.isLibraryCached(library)
}

internal class CacheLlvmModuleSpecification(
        cachedLibraries: CachedLibraries,
        private val libraryToCache: PartialCacheInfo,
        private val containsStdlib: Boolean,
) : LlvmModuleSpecificationBase(cachedLibraries) {
    override val isFinal = false

    override fun containsLibrary(library: KotlinLibrary): Boolean = library == libraryToCache.klib

    override fun containsDeclaration(declaration: IrDeclaration): Boolean {
        if (containsStdlib && libraryToCache.strategy.containsKFunctionImpl && declaration.getPackageFragment().isFunctionInterfaceFile)
            return true
        if (!super.containsDeclaration(declaration)) return false
        return (libraryToCache.strategy as? CacheDeserializationStrategy.SingleFile)
                ?.filePath.let { it == null || it == declaration.fileOrNull?.path }
    }
}
