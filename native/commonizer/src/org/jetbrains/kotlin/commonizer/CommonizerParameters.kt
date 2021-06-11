/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.konan.NativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.mergedtree.CirFictitiousFunctionClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.Logger

data class CommonizerParameters(
    val outputTargets: Set<SharedCommonizerTarget>,
    val manifestProvider: TargetDependent<NativeManifestDataProvider>,
    val dependenciesProvider: TargetDependent<ModulesProvider?>,
    val targetProviders: TargetDependent<TargetProvider?>,
    val resultsConsumer: ResultsConsumer,
    val storageManager: StorageManager = LockBasedStorageManager.NO_LOCKS,
    val statsCollector: StatsCollector? = null,
    val logger: Logger? = null,
)

internal fun CommonizerParameters.dependencyClassifiers(target: CommonizerTarget): CirProvidedClassifiers {
    val modulesProvider = dependenciesProvider[target]
    return CirProvidedClassifiers.of(CirFictitiousFunctionClassifiers, CirProvidedClassifiers.by(modulesProvider))
}
