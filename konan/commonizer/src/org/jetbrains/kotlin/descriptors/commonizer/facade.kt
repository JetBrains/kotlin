/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor
import org.jetbrains.kotlin.descriptors.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.mergeRoots
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class CommonizationParameters {
    // use linked hash map to preserve order
    private val modulesByTargets = LinkedHashMap<InputTarget, Collection<ModuleDescriptor>>()

    fun addTarget(target: InputTarget, modules: Collection<ModuleDescriptor>): CommonizationParameters {
        require(target !in modulesByTargets) { "Target $target is already added" }

        val modulesWithUniqueNames = modules.groupingBy { it.name }.eachCount()
        require(modulesWithUniqueNames.size == modules.size) {
            "Modules with duplicated names found: ${modulesWithUniqueNames.filter { it.value > 1 }}"
        }

        modulesByTargets[target] = modules

        return this
    }

    // get them as ordered immutable collection (List) for further processing
    fun getModulesByTargets(): List<Pair<InputTarget, Collection<ModuleDescriptor>>> =
        modulesByTargets.map { it.key to it.value }

    fun hasIntersection(): Boolean {
        if (modulesByTargets.size < 2)
            return false

        return modulesByTargets.flatMap { it.value }
            .groupingBy { it.name }
            .eachCount()
            .any { it.value == modulesByTargets.size }
    }
}

sealed class CommonizationResult

object NothingToCommonize : CommonizationResult()

class CommonizationPerformed(
    val modulesByTargets: Map<Target, Collection<ModuleDescriptor>>
) : CommonizationResult() {
    val commonTarget: OutputTarget by lazy {
        modulesByTargets.keys.filterIsInstance<OutputTarget>().single()
    }

    val concreteTargets: Set<InputTarget> by lazy {
        modulesByTargets.keys.filterIsInstance<InputTarget>().toSet()
    }
}

fun runCommonization(parameters: CommonizationParameters): CommonizationResult {
    if (!parameters.hasIntersection())
        return NothingToCommonize

    // build merged tree:
    val storageManager = LockBasedStorageManager("Declaration descriptors commonization")
    val mergedTree = mergeRoots(storageManager, parameters.getModulesByTargets())

    // commonize:
    mergedTree.accept(CommonizationVisitor(mergedTree), Unit)

    val modulesByTargets = LinkedHashMap<Target, Collection<ModuleDescriptor>>() // use linked hash map to preserve order

    // build resulting descriptors:
    val visitor = DeclarationsBuilderVisitor(storageManager) { target, commonizedModules ->
        check(target !in modulesByTargets)
        modulesByTargets[target] = commonizedModules
    }
    mergedTree.accept(visitor, DeclarationsBuilderVisitor.noContainingDeclarations())

    return CommonizationPerformed(modulesByTargets)
}
