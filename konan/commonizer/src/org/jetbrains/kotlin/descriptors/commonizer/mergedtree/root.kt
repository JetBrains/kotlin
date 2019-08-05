/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.ConcreteTargetId
import org.jetbrains.kotlin.descriptors.commonizer.ir.RootNode
import org.jetbrains.kotlin.descriptors.commonizer.ir.buildRootNode
import org.jetbrains.kotlin.name.Name

internal fun mergeRoots(modulesByTargets: List<Pair<ConcreteTargetId, Collection<ModuleDescriptor>>>): RootNode {
    val node = buildRootNode(modulesByTargets.map { it.first })

    val modulesMap = CommonizedGroupMap<Name, ModuleDescriptor>(modulesByTargets.size)

    modulesByTargets.forEachIndexed { index, (_, modules) ->
        for (module in modules) {
            modulesMap[module.name][index] = module
        }
    }

    for ((_, modulesGroup) in modulesMap) {
        node.modules += mergeModules(modulesGroup.toList())
    }

    return node
}
