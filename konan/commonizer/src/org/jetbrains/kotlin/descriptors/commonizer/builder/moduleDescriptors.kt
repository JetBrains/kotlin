/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirModule
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirModuleNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl

internal fun CirModuleNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ModuleDescriptorImpl>
) {
    target.forEachIndexed { index, module ->
        module?.buildDescriptor(components, output, index)
    }

    common()?.buildDescriptor(components, output, indexOfCommon)
}

private fun CirModule.buildDescriptor(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ModuleDescriptorImpl>,
    index: Int
) {
    val moduleDescriptor = ModuleDescriptorImpl(
        moduleName = name,
        storageManager = components.storageManager,
        builtIns = builtIns,
        capabilities = emptyMap() // TODO: preserve capabilities from the original module descriptors, KT-33998
    )

    output[index] = moduleDescriptor
}
