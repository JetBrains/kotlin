/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module

// Legacy stuff that will be dropped anyway in KT-65380

// TODO: remove it, KT-65380
private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
    val result = mutableSetOf<FqName>()
    val packageFragmentProvider = (module as? ModuleDescriptorImpl)?.packageFragmentProviderForModuleContentWithoutDependencies

    fun getSubPackages(fqName: FqName) {
        result.add(fqName)
        val subPackages = packageFragmentProvider?.getSubPackagesOf(fqName) { true }
                ?: module.getSubPackagesOf(fqName) { true }
        subPackages.forEach { getSubPackages(it) }
    }

    getSubPackages(FqName.ROOT)
    return result
}

// TODO: remove it, KT-65380
internal fun ModuleDescriptor.getPackageFragments(): List<PackageFragmentDescriptor> =
        getPackagesFqNames(this).flatMap {
            getPackage(it).fragments.filter { it.module == this }.toSet()
        }
