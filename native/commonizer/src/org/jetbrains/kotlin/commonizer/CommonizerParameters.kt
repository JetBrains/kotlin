/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.konan.NativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.stats.StatsCollector

class CommonizerParameters(
    val targetProviders: List<TargetProvider>,
    val resultsConsumer: ResultsConsumer,
    val commonManifestProvider: NativeManifestDataProvider,
    val commonDependencyModulesProvider: ModulesProvider? = null,
    val statsCollector: StatsCollector? = null,
    val progressLogger: ((String) -> Unit)? = null
) {
    internal val commonTarget: SharedCommonizerTarget = SharedCommonizerTarget(targetProviders.map { it.target }.toSet())

    internal val manifestDataProvider: TargetDependent<NativeManifestDataProvider>
        get() = TargetDependent { target ->
            if (target == commonTarget) return@TargetDependent commonManifestProvider
            this.targetProviders.firstOrNull { it.target == target }?.manifestProvider?.let { return@TargetDependent it }
            null
        }

}

fun CommonizerParameters.getCommonModuleNames(): Set<String> {
    if (targetProviders.size < 2) return emptySet() // too few targets

    val allModuleNames: List<Set<String>> = targetProviders.map { targetProvider ->
        targetProvider.modulesProvider.loadModuleInfos().mapTo(HashSet()) { it.name }
    }

    return allModuleNames.reduce { a, b -> a intersect b } // there are modules that are present in every target
}


