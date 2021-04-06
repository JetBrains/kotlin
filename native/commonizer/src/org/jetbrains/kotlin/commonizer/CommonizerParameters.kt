/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.konan.NativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.mergedtree.CirFictitiousFunctionClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.stats.StatsCollector

class CommonizerParameters(
    val outputTarget: SharedCommonizerTarget,
    val manifestProvider: TargetDependent<NativeManifestDataProvider>,
    val dependenciesProvider: TargetDependent<ModulesProvider?>,
    val targetProviders: TargetDependent<TargetProvider?>,
    val resultsConsumer: ResultsConsumer,
    val statsCollector: StatsCollector? = null,
    val progressLogger: ((String) -> Unit)? = null,
)

fun CommonizerParameters.getCommonModuleNames(): Set<String> {
    val supportedTargets = targetProviders.filterNonNull()
    if (supportedTargets.size < 2) return emptySet() // too few targets

    val allModuleNames: List<Set<String>> = supportedTargets.toList().map { targetProvider ->
        targetProvider.modulesProvider.loadModuleInfos().mapTo(HashSet()) { it.name }
    }

    return allModuleNames.reduce { a, b -> a intersect b } // there are modules that are present in every target
}

internal fun CommonizerParameters.dependencyClassifiers(target: CommonizerTarget): CirProvidedClassifiers {
    val modules = outputTarget.withAllAncestors()
        .sortedBy { it.level }
        .filter { it == target || it isAncestorOf target }
        .mapNotNull { compatibleTarget -> dependenciesProvider[compatibleTarget] }

    return modules.fold<ModulesProvider, CirProvidedClassifiers>(CirFictitiousFunctionClassifiers) { classifiers, module ->
        CirProvidedClassifiers.of(classifiers, CirProvidedClassifiers.by(module))
    }
}
