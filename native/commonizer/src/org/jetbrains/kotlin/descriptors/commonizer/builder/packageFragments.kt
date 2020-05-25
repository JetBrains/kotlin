/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackage
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirPackageNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.name.FqName

internal fun CirPackageNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<CommonizedPackageFragmentDescriptor>,
    modules: List<ModuleDescriptorImpl?>
) {
    target.forEachIndexed { index, pkg ->
        pkg?.buildDescriptor(components, output, index, modules)
    }

    common()?.buildDescriptor(components, output, indexOfCommon, modules)
}

private fun CirPackage.buildDescriptor(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<CommonizedPackageFragmentDescriptor>,
    index: Int,
    modules: List<ModuleDescriptorImpl?>
) {
    val module = modules[index] ?: error("No containing declaration for package $this")

    val packageFragment = CommonizedPackageFragmentDescriptor(module, fqName)

    // cache created package fragment descriptor:
    components.cache.cache(module.name, fqName, index, packageFragment)

    output[index] = packageFragment
}

class CommonizedPackageFragmentDescriptor(
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName) {
    private lateinit var memberScope: CommonizedMemberScope

    fun initialize(memberScope: CommonizedMemberScope) {
        this.memberScope = memberScope
    }

    override fun getMemberScope(): CommonizedMemberScope = memberScope
}
