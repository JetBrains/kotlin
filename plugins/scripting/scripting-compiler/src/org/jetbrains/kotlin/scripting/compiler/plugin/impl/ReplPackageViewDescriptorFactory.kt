/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.impl.LazyPackageViewDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageViewDescriptorFactory
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.descriptors.packageFragments
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.StorageManager

object ReplPackageViewDescriptorFactory : PackageViewDescriptorFactory {
    override fun compute(module: ModuleDescriptorImpl, fqName: FqName, storageManager: StorageManager): PackageViewDescriptor {
        return ReplPackageViewDescriptor(module, fqName, storageManager)
    }
}

class ReplPackageViewDescriptor(
    module: ModuleDescriptorImpl,
    fqName: FqName,
    storageManager: StorageManager
) : LazyPackageViewDescriptorImpl(module, fqName, storageManager) {
    private var cachedFragments: List<PackageFragmentDescriptor>? = null

    override val fragments: List<PackageFragmentDescriptor>
        get() {
            cachedFragments?.let { return it }
            val calculatedFragments = module.packageFragmentProvider.packageFragments(fqName)
            if (calculatedFragments.isNotEmpty()) cachedFragments = calculatedFragments
            return calculatedFragments
        }

    override fun isEmpty(): Boolean {
        cachedFragments?.let { return it.isEmpty() }
        return module.packageFragmentProvider.isEmpty(fqName)
    }
}
