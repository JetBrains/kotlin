/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlinx.metadata.klib.ChunkedKlibModuleFragmentWriteStrategy
import org.jetbrains.kotlin.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.commonizer.ResultsConsumer.ModuleResult.Missing
import org.jetbrains.kotlin.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.commonizer.mergedtree.CirCommonizedClassifierNodes
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.commonizer.mergedtree.CirNode.Companion.targetIndices
import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.commonizer.metadata.CirTreeSerializer.serializeSingleTarget
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot
import org.jetbrains.kotlin.commonizer.tree.assembleCirTree
import org.jetbrains.kotlin.commonizer.tree.deserializeCirTree
import org.jetbrains.kotlin.commonizer.tree.mergeCirTree
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

fun runCommonization(parameters: CommonizerParameters) {
    if (!parameters.containsCommonModuleNames()) {
        parameters.resultsConsumer.allConsumed(parameters, Status.NOTHING_TO_DO)
        return
    }

    val storageManager = LockBasedStorageManager("Declarations commonization")
    commonize(parameters, storageManager, parameters.outputTarget)
    parameters.resultsConsumer.allConsumed(parameters, Status.DONE)
}

private fun commonize(
    parameters: CommonizerParameters,
    storageManager: StorageManager,
    target: SharedCommonizerTarget,
): CirRootNode? {
    val cirTrees = getCirTree(parameters, storageManager, target)
    parameters.logProgress("Built declaration tree for $target")

    // build merged tree:
    val classifiers = CirKnownClassifiers(
        commonizedNodes = CirCommonizedClassifierNodes.default(),
        commonDependencies = parameters.dependencyClassifiers(target)
    )

    val mergedTree = merge(storageManager, classifiers, cirTrees) ?: return null
    mergedTree.accept(CommonizationVisitor(classifiers, mergedTree), Unit)
    parameters.logProgress("Commonized declarations for $target")

    serialize(parameters, mergedTree, target)

    return mergedTree
}

private fun getCirTree(
    parameters: CommonizerParameters, storageManager: StorageManager, target: SharedCommonizerTarget
): TargetDependent<CirTreeRoot?> {
    return EagerTargetDependent(target.targets) { childTarget ->
        when (childTarget) {
            is LeafCommonizerTarget -> deserialize(parameters, childTarget)
            is SharedCommonizerTarget -> commonize(parameters.fork(), storageManager, childTarget)?.assembleCirTree().also {
                parameters.logProgress("Commonized target $childTarget")
            }
        }
    }
}

private fun deserialize(parameters: CommonizerParameters, target: CommonizerTarget): CirTreeRoot? {
    val targetProvider = parameters.targetProviders[target] ?: return null
    return deserializeCirTree(parameters, targetProvider)
}

private fun merge(
    storageManager: StorageManager, classifiers: CirKnownClassifiers, cirTrees: TargetDependent<CirTreeRoot?>,
): CirRootNode? {
    val availableTrees = cirTrees.filterNonNull()
    /* Nothing to merge */
    if (availableTrees.size == 0) return null

    return mergeCirTree(storageManager, classifiers, availableTrees)
}

private fun serialize(parameters: CommonizerParameters, mergedTree: CirRootNode, commonTarget: CommonizerTarget) {
    for (targetIndex in mergedTree.targetIndices) {
        val target = mergedTree.targetDeclarations[targetIndex]?.target ?: continue
        serializeMissingModules(parameters, target)
        if (target is LeafCommonizerTarget) {
            serialize(parameters, mergedTree, target, targetIndex)
        }
    }
    serialize(parameters, mergedTree, commonTarget, mergedTree.indexOfCommon)
}

private fun serialize(parameters: CommonizerParameters, mergedTree: CirRootNode, target: CommonizerTarget, targetIndex: Int) {
    serializeSingleTarget(mergedTree, targetIndex, parameters.statsCollector) { metadataModule ->
        val libraryName = metadataModule.name
        val serializedMetadata = with(metadataModule.write(KLIB_FRAGMENT_WRITE_STRATEGY)) {
            SerializedMetadata(header, fragments, fragmentNames)
        }
        val manifestData = parameters.manifestProvider[target].buildManifest(libraryName)
        parameters.resultsConsumer.consume(parameters, target, ModuleResult.Commonized(libraryName, serializedMetadata, manifestData))
    }
    parameters.resultsConsumer.targetConsumed(parameters, target)
}

private fun serializeMissingModules(parameters: CommonizerParameters, requestedTarget: CommonizerTarget) {
    val targetProvider = parameters.targetProviders.getOrNull(requestedTarget) ?: return
    val commonModuleNames = parameters.commonModuleNames(targetProvider)

    targetProvider.modulesProvider.loadModuleInfos()
        .filter { it.name !in commonModuleNames }
        .forEach { missingModule ->
            parameters.resultsConsumer.consume(parameters, requestedTarget, Missing(missingModule.originalLocation))
        }
}

private fun CommonizerParameters.fork(): CommonizerParameters = with(logger?.fork())

private fun CommonizerParameters.logProgress(message: String) = logger?.progress(message)

private val KLIB_FRAGMENT_WRITE_STRATEGY = ChunkedKlibModuleFragmentWriteStrategy()

