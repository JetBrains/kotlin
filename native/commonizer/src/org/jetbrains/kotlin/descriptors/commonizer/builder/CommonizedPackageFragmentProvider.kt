/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class CommonizedPackageFragmentProvider : PackageFragmentProvider {
    private val packageFragments = ArrayList<PackageFragmentDescriptor>()

    operator fun plusAssign(packageFragment: PackageFragmentDescriptor) {
        this.packageFragments += packageFragment
    }

    override fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>) {
        this.packageFragments.filterTo(packageFragments) { it.fqName == fqName }
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> =
        packageFragments.asSequence()
            .map { it.fqName }
            .filter { !it.isRoot && it.parent() == fqName }
            .toList()

    companion object {
        fun createArray(size: Int) = Array(size) { CommonizedPackageFragmentProvider() }

        operator fun Array<CommonizedPackageFragmentProvider>.plusAssign(packageFragments: List<PackageFragmentDescriptor?>) {
            packageFragments.forEachIndexed { index, packageFragment ->
                this[index] += packageFragment ?: return@forEachIndexed
            }
        }
    }
}
