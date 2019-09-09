/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Package
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.PackageNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope

internal fun PackageNode.buildDescriptors(
    output: CommonizedGroup<CommonizedPackageFragmentDescriptor>,
    modules: List<ModuleDescriptorImpl?>
) {
    target.forEachIndexed { index, pkg ->
        pkg?.buildDescriptor(output, index, modules)
    }

    common()?.buildDescriptor(output, indexOfCommon, modules)
}

private fun Package.buildDescriptor(
    output: CommonizedGroup<CommonizedPackageFragmentDescriptor>,
    index: Int,
    modules: List<ModuleDescriptorImpl?>
) {
    val module = modules[index] ?: error("No containing declaration for package $this")
    val packageFragment = CommonizedPackageFragmentDescriptor(module, fqName)
    output[index] = packageFragment
}

internal class CommonizedPackageFragmentDescriptor(
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName) {
    private lateinit var memberScope: MemberScope

    fun initialize(memberScope: MemberScope) {
        this.memberScope = memberScope
    }

    override fun getMemberScope() = memberScope
}
