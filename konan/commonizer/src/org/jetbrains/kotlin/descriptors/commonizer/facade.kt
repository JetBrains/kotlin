/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor
import org.jetbrains.kotlin.descriptors.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.mergeRoots
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class CommonizationSession {
    // TODO (???): add progress tracker
    // TODO (???): add logger
}

class CommonizationParameters {
    private val modulesByTargets = LinkedHashMap<ConcreteTargetId, Collection<ModuleDescriptor>>()

    fun addTarget(targetName: String, modules: Collection<ModuleDescriptor>): CommonizationParameters {
        val targetId = ConcreteTargetId(targetName)
        require(targetId !in modulesByTargets) {
            "Target $targetId is already added"
        }

        val modulesWithUniqueNames = modules.groupingBy { it.name }.eachCount()
        require(modulesWithUniqueNames.size == modules.size) {
            "Modules with duplicated names found: ${modulesWithUniqueNames.filter { it.value > 1 }}"
        }

        modulesByTargets[targetId] = modules

        return this
    }

    // get them as ordered immutable collection (List) for further processing
    fun getModulesByTargets(): List<Pair<ConcreteTargetId, Collection<ModuleDescriptor>>> =
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
    val commonModules: Collection<ModuleDescriptor>,
    val modulesByTargets: Map<String, Collection<ModuleDescriptor>>
) : CommonizationResult()

fun runCommonization(parameters: CommonizationParameters): CommonizationResult {
    if (!parameters.hasIntersection())
        return NothingToCommonize

    // build merged tree:
    val storageManager = LockBasedStorageManager("Declaration descriptors commonization")
    val mergedTree = mergeRoots(storageManager, parameters.getModulesByTargets())

    // commonize:
    mergedTree.accept(CommonizationVisitor(mergedTree), Unit)

    var commonModules: Collection<ModuleDescriptor>? = null
    val otherModulesByTargets = LinkedHashMap<String, Collection<ModuleDescriptor>>()

    // build resulting descriptors:
    val visitor = DeclarationsBuilderVisitor(storageManager, DefaultBuiltIns.Instance) { targetId, commonizedModules ->
        when (targetId) {
            is CommonTargetId -> {
                check(commonModules == null)
                commonModules = commonizedModules
            }
            is ConcreteTargetId -> {
                val targetName = targetId.name
                check(targetName !in otherModulesByTargets)
                otherModulesByTargets[targetName] = commonizedModules
            }
        }
    }
    mergedTree.accept(visitor, DeclarationsBuilderVisitor.noContainingDeclarations())

    return CommonizationPerformed(commonModules!!, otherModulesByTargets)
}
