/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

/**
 * @return Set of module names that is available across all children targets
 */
internal fun CommonizerParameters.commonModuleNames(target: CommonizerTarget): Set<String> {
    val supportedTargets = target.withAllLeaves().mapNotNull(targetProviders::getOrNull)
    if (supportedTargets.isEmpty()) return emptySet() // Nothing to do

    val allModuleNames: List<Set<String>> = supportedTargets.toList().map { targetProvider ->
        targetProvider.modulesProvider.moduleInfos.mapTo(HashSet()) { it.name }
    }

    return allModuleNames.reduce { a, b -> a intersect b } // there are modules that are present in every target
}

/**
 * @return Set of module names that this [targetProvider] shares with *at least* one other target
 */
internal fun CommonizerParameters.commonModuleNames(targetProvider: TargetProvider): Set<String> {
    return outputTargets
        .filter { target -> (target.allLeaves() intersect targetProvider.target.allLeaves()).isNotEmpty() }
        .map { target -> commonModuleNames(target) }
        .fold(emptySet()) { acc, names -> acc + names }
}


internal fun CommonizerParameters.containsCommonModuleNames(): Boolean {
    return targetProviders.filterNonNull().any { targetProvider -> commonModuleNames(targetProvider).isNotEmpty() }
}
