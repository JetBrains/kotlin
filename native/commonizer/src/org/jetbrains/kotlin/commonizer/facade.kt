/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlinx.metadata.klib.ChunkedKlibModuleFragmentWriteStrategy
import org.jetbrains.kotlin.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.commonizer.metadata.CirTreeSerializer
import org.jetbrains.kotlin.commonizer.tree.deserializer.deserialize
import org.jetbrains.kotlin.commonizer.tree.merger.mergeCirTree
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

fun runCommonization(parameters: CommonizerParameters) {
    if (parameters.getCommonModuleNames().isEmpty()) {
        parameters.resultsConsumer.allConsumed(Status.NOTHING_TO_DO)
        return
    }

    val storageManager = LockBasedStorageManager("Declarations commonization")

    val mergedTree = mergeAndCommonize(storageManager, parameters)

    // build resulting declarations:
    for (targetIndex in 0 until mergedTree.dimension) {
        serializeTarget(mergedTree, targetIndex, parameters)
    }

    parameters.resultsConsumer.allConsumed(Status.DONE)
}

private fun mergeAndCommonize(storageManager: StorageManager, parameters: CommonizerParameters): CirRootNode {
    // build merged tree:
    val classifiers = CirKnownClassifiers(
        commonizedNodes = CirCommonizedClassifierNodes.default(),
        commonDependencies = CirProvidedClassifiers.of(
            CirFictitiousFunctionClassifiers,
            CirProvidedClassifiers.by(parameters.commonDependencyModulesProvider)
        )
    )

    val targetTrees = parameters.targetProviders.mapValue { target -> deserialize(parameters, target) }
    parameters.progressLogger?.invoke("Deserialized targets")

    val mergedTree = mergeCirTree(storageManager, classifiers, targetTrees)
    mergedTree.accept(CommonizationVisitor(classifiers, mergedTree), Unit)
    parameters.progressLogger?.invoke("Commonized declarations")

    return mergedTree
}

private fun serializeTarget(mergedTree: CirRootNode, targetIndex: Int, parameters: CommonizerParameters) {
    val target = mergedTree.getTarget(targetIndex)

    CirTreeSerializer.serializeSingleTarget(mergedTree, targetIndex, parameters.statsCollector) { metadataModule ->
        val libraryName = metadataModule.name
        val serializedMetadata = with(metadataModule.write(KLIB_FRAGMENT_WRITE_STRATEGY)) {
            SerializedMetadata(header, fragments, fragmentNames)
        }
        val manifestData = parameters.manifestDataProvider[target].getManifest(libraryName)
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
