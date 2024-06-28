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
    fun guess(
        moduleDescriptor: ModuleDescriptor,
        includedLibraryDescriptors: List<ModuleDescriptor>,
        exportedDependencies: List<ModuleDescriptor>,
    ): FqName {
        // Consider exported libraries only if we cannot infer the package from sources or included libs.
        return guessMainPackage(includedLibraryDescriptors + moduleDescriptor)
            ?: guessMainPackage(exportedDependencies)
            ?: FqName.ROOT
    }

    private fun guessMainPackage(modules: List<ModuleDescriptor>): FqName? {
        if (modules.isEmpty()) {
            return null
        }

        val allPackages = modules.flatMap {
            it.getPackageFragments() // Includes also all parent packages, e.g. the root one.
        }

        val nonEmptyPackages = allPackages
            .filter { it.getMemberScope().getContributedDescriptors().isNotEmpty() }
            .map { it.fqName }.distinct()

        return allPackages.map { it.fqName }.distinct()
            .filter { candidate -> nonEmptyPackages.all { it.isSubpackageOf(candidate) } }
            // Now there are all common ancestors of non-empty packages. Longest of them is the least common accessor:
            .maxByOrNull { it.asString().length }
    }
}