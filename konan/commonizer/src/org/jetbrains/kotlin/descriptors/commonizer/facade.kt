/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.builder.*
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor1
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor2
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

    val storageManager = LockBasedStorageManager("Declaration descriptors commonization")

    // build merged tree:
    val mergedTree = mergeRoots(storageManager, parameters.getModulesByTargets())

    // commonize:
    mergedTree.accept(CommonizationVisitor(mergedTree), Unit)

    // build resulting descriptors:
    val components = mergedTree.createGlobalBuilderComponents(storageManager)
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
