/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree.mergers

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.storage.StorageManager

internal class RootMerger(
    private val targetMerger: TargetMerger
) {
    fun processRoot(
        storageManager: StorageManager,
        classifiers: CirKnownClassifiers,
        parameters: CommonizerParameters
    ): CirTreeMergeResult {
        val rootNode: CirRootNode = buildRootNode(storageManager, parameters.targetProviders.size)

        val commonModuleNames = parameters.getCommonModuleNames()
        val missingModuleInfosByTargets = mutableMapOf<CommonizerTarget, Collection<ModulesProvider.ModuleInfo>>()

        parameters.targetProviders.filterNotNull().forEachIndexed { targetIndex, targetProvider ->
            val allModuleInfos = targetProvider.modulesProvider.loadModuleInfos()

            val (commonModuleInfos, missingModuleInfos) = allModuleInfos.partition { it.name in commonModuleNames }
            val typeResolver = CirTypeResolver.create(
                providedClassifiers = CirProvidedClassifiers.of(
                    CirProvidedClassifiers.by(targetProvider.modulesProvider),
                    parameters.dependencyClassifiers(targetProvider.target)
                )
            )
            targetMerger.processTarget(
                CirTargetMergingContext(
                    storageManager, classifiers, parameters, targetIndex, typeResolver
                ), rootNode, targetProvider, commonModuleInfos
            )

            missingModuleInfosByTargets[targetProvider.target] = missingModuleInfos

            parameters.progressLogger?.invoke("Loaded declarations for ${targetProvider.target.prettyName}")
            System.gc()
        }

        return CirTreeMergeResult(
            root = rootNode,
            missingModuleInfos = missingModuleInfosByTargets
        )
    }
}
