/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.serialization.NativeCacheSupport
import org.jetbrains.kotlin.backend.konan.serialization.isFromCInteropLibrary
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.library.KotlinLibrary

class NativeCacheSupportImpl(
        override val cachedLibraries: CachedLibraries,
        override val lazyIrForCaches: Boolean,
        override val libraryBeingCached: PartialCacheInfo?,
) : NativeCacheSupport {
    override fun getDescriptorForCachedDeclarationModuleDeserializer(declaration: IrDeclaration): ModuleDescriptor? {
        val packageFragment = declaration.getPackageFragment()
        val moduleDescriptor = packageFragment.moduleDescriptor
        val klib = packageFragment.konanLibrary
        val declarationBeingCached = packageFragment is IrFile && klib != null &&
                libraryBeingCached?.let {
                    it.klib == klib && it.strategy.contains(packageFragment.path)
                } == true
        return if (klib != null && !moduleDescriptor.isFromCInteropLibrary()
                && cachedLibraries.isLibraryCached(klib) && !declarationBeingCached)
            moduleDescriptor
        else null
    }

    override fun getDeserializationStrategy(klib: KotlinLibrary): CacheDeserializationStrategy = when {
        klib == libraryBeingCached?.klib -> libraryBeingCached.strategy
        lazyIrForCaches && cachedLibraries.isLibraryCached(klib) -> CacheDeserializationStrategy.Nothing
        else -> CacheDeserializationStrategy.WholeModule
    }
}
