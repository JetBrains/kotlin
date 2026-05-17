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
import org.jetbrains.kotlin.backend.konan.serialization.CacheDeserializationStrategy
import org.jetbrains.kotlin.backend.konan.serialization.PartialCacheInfo
import org.jetbrains.kotlin.ir.IrBasedFunctionFactory.Companion.isFunctionInterfaceFile
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

/**
 * Spec for the device fragment of a CUDA-aware compilation. The LLVM module produced for
 * this fragment becomes a standalone PTX text blob — it cannot link against host caches or
 * other libraries' object code.
 *
 * `containsLibrary` returns true for libraries whose files include a `@CudaCompile`-annotated
 * file (and `false` for everything else — host stdlib, cinterop klibs, etc.). The set is
 * computed once at split time and passed in. `findDependenciesToCompile` consults this to
 * decide which libraries' IrModuleFragments to expose to `mergeDependencies`, which then
 * applies the per-file `@CudaCompile` filter. Returning `false` for everything (as an earlier
 * iteration did) made `mergeDependencies` see no candidate libraries and left the device
 * fragment empty.
 *
 * Main-module declarations (the user's `@CudaCompile`-annotated files) carry
 * `konanLibrary == null` and are accepted by the base class without consulting this method.
 * `kotlin.native.cuda` intrinsics are `@GCUnsafeCall`-intrinsified at the call site and never
 * have their bodies emitted.
 *
 * For level-3 device-helper sharing (CUDA code shipped in a klib referenced by another
 * library), this set is naturally the transitive closure of klibs with CUDA content — same
 * shape, just larger.
 */
internal class CudaDeviceLlvmModuleSpecification(
        cachedLibraries: CachedLibraries,
        private val containedLibraries: Set<KotlinLibrary>,
) : LlvmModuleSpecificationBase(cachedLibraries) {
    override val isFinal = true

    override fun containsLibrary(library: KotlinLibrary): Boolean = library in containedLibraries
}
