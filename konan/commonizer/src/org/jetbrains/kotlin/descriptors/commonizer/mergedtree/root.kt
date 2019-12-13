/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.InputTarget
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirRootNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.buildRootNode
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroupMap
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager

internal fun mergeRoots(
    storageManager: StorageManager,
    modulesByTargets: List<Pair<InputTarget, Collection<ModuleDescriptor>>>
): CirRootNode {
    val node = buildRootNode(storageManager, modulesByTargets.map { it.first })

    val modulesMap =
        CommonizedGroupMap<Name, ModuleDescriptor>(modulesByTargets.size)

    modulesByTargets.forEachIndexed { index, (_, modules) ->
        for (module in modules) {
            modulesMap[module.name][index] = module
        }
    }

    for ((_, modulesGroup) in modulesMap) {
        node.modules += mergeModules(storageManager, node.cache, modulesGroup.toList())
    }

    return node
}
