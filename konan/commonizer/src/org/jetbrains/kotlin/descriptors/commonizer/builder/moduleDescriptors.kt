/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Module
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ModuleNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.storage.StorageManager

internal fun ModuleNode.buildDescriptors(
    output: CommonizedGroup<ModuleDescriptorImpl>,
    storageManager: StorageManager,
    builtIns: KotlinBuiltIns
) {
    target.forEachIndexed { index, module ->
        module?.buildDescriptor(output, index, storageManager, builtIns)
    }

    common()?.buildDescriptor(output, indexOfCommon, storageManager, builtIns)
}

private fun Module.buildDescriptor(
    output: CommonizedGroup<ModuleDescriptorImpl>,
    index: Int,
    storageManager: StorageManager,
    builtIns: KotlinBuiltIns
) {
    val moduleDescriptor = ModuleDescriptorImpl(
        moduleName = name,
        storageManager = storageManager,
        builtIns = builtIns,
        capabilities = emptyMap() // TODO: specify capabilities
    )

    output[index] = moduleDescriptor
}
