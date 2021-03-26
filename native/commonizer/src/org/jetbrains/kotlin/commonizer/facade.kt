/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlinx.metadata.klib.ChunkedKlibModuleFragmentWriteStrategy
import org.jetbrains.kotlin.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.commonizer.mergedtree.CirCommonizedClassifierNodes
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.commonizer.mergedtree.CirNode.Companion.indices
import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.commonizer.metadata.CirTreeSerializer
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot
import org.jetbrains.kotlin.commonizer.tree.assembleCirTree
import org.jetbrains.kotlin.commonizer.tree.deserializeCirTree
import org.jetbrains.kotlin.commonizer.tree.mergeCirTree
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

fun runCommonization(parameters: CommonizerParameters) {
    if (parameters.getCommonModuleNames().isEmpty()) {
        parameters.resultsConsumer.allConsumed(Status.NOTHING_TO_DO)
        return
    }

    val storageManager = LockBasedStorageManager("Declarations commonization")
    serializeCommonDeclarations(commonize(parameters, storageManager, parameters.outputTarget), parameters)
    parameters.resultsConsumer.allConsumed(Status.DONE)
}

private fun commonize(
    parameters: CommonizerParameters,
    storageManager: StorageManager,
    target: SharedCommonizerTarget,
): CirRootNode {
    val cirTrees = getCirTree(parameters, storageManager, target)
    parameters.progressLogger?.invoke("Build cir tree for $target")

    // build merged tree:
    val classifiers = CirKnownClassifiers(
        commonizedNodes = CirCommonizedClassifierNodes.default(),
        commonDependencies = parameters.dependencyClassifiers(target)
    )

    val mergedTree = mergeCirTree(storageManager, classifiers, cirTrees)
    mergedTree.accept(CommonizationVisitor(classifiers, mergedTree), Unit)
    parameters.progressLogger?.invoke("Commonized declarations for $target")

    serializeTargetDeclarations(mergedTree, parameters)
    parameters.progressLogger?.invoke("Commonized target $target")

    return mergedTree
}

private fun getCirTree(
    parameters: CommonizerParameters, storageManager: StorageManager, target: SharedCommonizerTarget
): TargetDependent<CirTreeRoot> {
    return TargetDependent(target.targets) { childTarget ->
        when (childTarget) {
            is LeafCommonizerTarget -> deserializeCirTree(parameters, parameters.targetProviders[childTarget])
            is SharedCommonizerTarget -> commonize(parameters, storageManager, childTarget).assembleCirTree()
        }
    }
}

private fun serializeTargetDeclarations(mergedTree: CirRootNode, parameters: CommonizerParameters) {
    for (targetIndex in (mergedTree.indices - mergedTree.indexOfCommon)) {
        serializeTarget(mergedTree, targetIndex, parameters)
    }
}

private fun serializeCommonDeclarations(mergedTree: CirRootNode, parameters: CommonizerParameters) {
    serializeTarget(mergedTree, mergedTree.indexOfCommon, parameters)
}

private fun serializeTarget(mergedTree: CirRootNode, targetIndex: Int, parameters: CommonizerParameters) {
    val target = mergedTree.getTarget(targetIndex)

    CirTreeSerializer.serializeSingleTarget(mergedTree, targetIndex, parameters.statsCollector) { metadataModule ->
        val libraryName = metadataModule.name
        val serializedMetadata = with(metadataModule.write(KLIB_FRAGMENT_WRITE_STRATEGY)) {
            SerializedMetadata(header, fragments, fragmentNames)
        }
        val manifestData = parameters.manifestProvider[target].getManifest(libraryName)
        parameters.resultsConsumer.consume(target, ModuleResult.Commonized(libraryName, serializedMetadata, manifestData))
    }

    if (target in parameters.targetProviders.targets) {
        parameters.targetProviders[target].modulesProvider.loadModuleInfos()
            .filter { it.name !in parameters.getCommonModuleNames() }
            .forEach { missingModule -> parameters.resultsConsumer.consume(target, ModuleResult.Missing(missingModule.originalLocation)) }
    }

    parameters.resultsConsumer.targetConsumed(target)
}

private val KLIB_FRAGMENT_WRITE_STRATEGY = ChunkedKlibModuleFragmentWriteStrategy()
