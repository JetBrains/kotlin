/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.CachedLibraries
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * A wrapper class around [KonanIrLinker] that provides access to [KonanPartialModuleDeserializer].
 * Usually this is needed to access data stored in compiler caches like class layout information.
 */
internal class ModuleDeserializerProvider(
        private val libraryBeingCached: PartialCacheInfo?,
        private val cachedLibraries: CachedLibraries,
        private val linker: KonanIrLinker,
) {
    /**
     * @return a [KonanPartialModuleDeserializer] for the klib that contains the given [declaration]. Null if the [declaration] is a part
     * of a library currently being cached, or it is from cinterop library.
     */
    fun getDeserializerOrNull(declaration: IrDeclaration): KonanPartialModuleDeserializer? {
        val packageFragment = declaration.getPackageFragment()
        val moduleDescriptor = packageFragment.moduleDescriptor
        val klib = packageFragment.konanLibrary
        val declarationBeingCached = packageFragment is IrFile && klib != null && libraryBeingCached?.klib == klib
                && libraryBeingCached.strategy.contains(packageFragment.path)
        return if (klib != null && !moduleDescriptor.isFromCInteropLibrary() && cachedLibraries.isLibraryCached(klib) && !declarationBeingCached) {
            linker.moduleDeserializers[moduleDescriptor] ?: error("No module deserializer for ${declaration.render()}")
        } else {
            null
        }
    }

    /**
     * @return a [KonanPartialModuleDeserializer] for the corresponding [library]. Null if it is a cinterop library.
     */
    fun getDeserializerOrNull(library: KotlinLibrary): KonanPartialModuleDeserializer? {
        return linker.klibToModuleDeserializerMap[library]
    }
}