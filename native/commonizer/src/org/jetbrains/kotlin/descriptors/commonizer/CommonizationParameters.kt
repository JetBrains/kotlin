/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor

class CommonizationParameters(
    val statsCollector: StatsCollector? = null
) {
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
