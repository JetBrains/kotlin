/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.TargetProvider
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager

internal fun mergeRoots(
    storageManager: StorageManager,
    targetProviders: List<TargetProvider>
): CirRootNode {
    val node = buildRootNode(storageManager, targetProviders)

    val modulesMap = CommonizedGroupMap<Name, ModuleDescriptor>(targetProviders.size)

    targetProviders.forEachIndexed { index, targetProvider ->
        for (module in targetProvider.modulesProvider.loadModules()) {
            modulesMap[module.name.intern()][index] = module
        }
    }

    for ((_, modulesGroup) in modulesMap) {
        node.modules += mergeModules(storageManager, node.cache, modulesGroup.toList())
    }

    return node
}
