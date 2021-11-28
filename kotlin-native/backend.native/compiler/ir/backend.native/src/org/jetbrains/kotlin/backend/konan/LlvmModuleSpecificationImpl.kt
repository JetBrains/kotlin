/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
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

    override fun containsDeclaration(declaration: IrDeclaration): Boolean =
            declaration.konanLibrary.let { it == null || containsLibrary(it) }
}

internal class DefaultLlvmModuleSpecification(cachedLibraries: CachedLibraries)
    : LlvmModuleSpecificationBase(cachedLibraries) {
    override val isFinal = true

    override fun containsLibrary(library: KotlinLibrary): Boolean = !cachedLibraries.isLibraryCached(library)
}

internal class CacheLlvmModuleSpecification(
        cachedLibraries: CachedLibraries,
        private val librariesToCache: Set<KotlinLibrary>
) : LlvmModuleSpecificationBase(cachedLibraries) {
    override val isFinal = false

    override fun containsLibrary(library: KotlinLibrary): Boolean = library in librariesToCache
}
