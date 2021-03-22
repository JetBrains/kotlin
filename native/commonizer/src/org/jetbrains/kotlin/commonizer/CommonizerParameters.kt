/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.konan.TargetedNativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.stats.StatsCollector

class CommonizerParameters(
    val resultsConsumer: ResultsConsumer,
    val manifestDataProvider: TargetedNativeManifestDataProvider,
    // common module dependencies (ex: Kotlin stdlib)
    val commonDependencyModulesProvider: ModulesProvider? = null,
    val statsCollector: StatsCollector? = null,
    val progressLogger: ((String) -> Unit)? = null
) {
    // use linked hash map to preserve order
    private val _targetProviders = LinkedHashMap<CommonizerTarget, TargetProvider>()
    val targetProviders: List<TargetProvider> get() = _targetProviders.values.toList()

    fun addTarget(targetProvider: TargetProvider): CommonizerParameters {
        require(targetProvider.target !in _targetProviders) { "Target ${targetProvider.target} is already added" }
        _targetProviders[targetProvider.target] = targetProvider

        return this
    }

    fun getCommonModuleNames(): Set<String> {
        if (_targetProviders.size < 2) return emptySet() // too few targets

        val allModuleNames: List<Set<String>> = _targetProviders.values.map { targetProvider ->
            targetProvider.modulesProvider.loadModuleInfos().mapTo(HashSet()) { it.name }
        }

        return allModuleNames.reduce { a, b -> a intersect b } // there are modules that are present in every target
    }

    fun hasAnythingToCommonize(): Boolean {
        return getCommonModuleNames().isNotEmpty()
    }
}
