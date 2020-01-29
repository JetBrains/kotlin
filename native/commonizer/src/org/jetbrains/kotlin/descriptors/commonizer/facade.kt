/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor1
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor2
import org.jetbrains.kotlin.descriptors.commonizer.builder.createGlobalBuilderComponents
import org.jetbrains.kotlin.descriptors.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.mergeRoots
import org.jetbrains.kotlin.storage.LockBasedStorageManager

fun runCommonization(parameters: CommonizationParameters): CommonizationResult {
    if (!parameters.hasIntersection())
        return NothingToCommonize

    val storageManager = LockBasedStorageManager("Declaration descriptors commonization")

    // build merged tree:
    val mergedTree = mergeRoots(storageManager, parameters.getModulesByTargets())

    // commonize:
    mergedTree.accept(CommonizationVisitor(mergedTree), Unit)

    // build resulting descriptors:
    val components = mergedTree.createGlobalBuilderComponents(storageManager, parameters.statsCollector)
    mergedTree.accept(DeclarationsBuilderVisitor1(components), emptyList())
    mergedTree.accept(DeclarationsBuilderVisitor2(components), emptyList())

    val modulesByTargets = LinkedHashMap<Target, Collection<ModuleDescriptor>>() // use linked hash map to preserve order
    components.targetComponents.forEach {
        val target = it.target
        check(target !in modulesByTargets)

        modulesByTargets[target] = components.cache.getAllModules(it.index)
    }

    return CommonizationPerformed(modulesByTargets)
}
