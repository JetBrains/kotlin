/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf

/**
 * Tries to infer a main package name that can be used
 * for bundle ID of a framework.
 */
@InternalKotlinNativeApi
class MainPackageGuesser {
    class ModulePackages(val commonNonEmptyPackage: FqName?, val longestPackage: FqName?)

    fun guess(
        moduleDescriptor: ModuleDescriptor,
        includedLibraryDescriptors: List<ModuleDescriptor>,
        exportedDependencies: List<ModuleDescriptor>,
    ): FqName {
        // Consider exported libraries only if we cannot infer the package from sources or included libs.
        return guessMainPackage((includedLibraryDescriptors + moduleDescriptor).map(::packagesOf))
            ?: guessMainPackage(exportedDependencies.map(::packagesOf))
            ?: FqName.ROOT
    }

    fun packagesOf(module: ModuleDescriptor): ModulePackages {
        val packages = module.getPackageFragments() // Includes also all parent packages, e.g. the root one.

        var commonNonEmptyPackage: FqName? = null
        for (fragment in packages) {
            val current = commonNonEmptyPackage
            if (current != null && fragment.fqName.isSubpackageOf(current)) continue
            if (fragment.getMemberScope().getContributedDescriptors().isEmpty()) continue
            commonNonEmptyPackage = current?.commonAncestorWith(fragment.fqName) ?: fragment.fqName
        }

        return ModulePackages(commonNonEmptyPackage, packages.map { it.fqName }.maxByOrNull { it.asString().length })
    }

    fun guessMainPackage(modules: List<ModulePackages>): FqName? {
        var commonNonEmptyPackage: FqName? = null
        for (module in modules) {
            val packageName = module.commonNonEmptyPackage ?: continue
            commonNonEmptyPackage = commonNonEmptyPackage?.commonAncestorWith(packageName) ?: packageName
        }
        return commonNonEmptyPackage ?: modules.mapNotNull { it.longestPackage }.maxByOrNull { it.asString().length }
    }

    private fun FqName.commonAncestorWith(other: FqName): FqName {
        var ancestor = this
        while (!other.isSubpackageOf(ancestor)) {
            ancestor = ancestor.parent()
        }
        return ancestor
    }
}