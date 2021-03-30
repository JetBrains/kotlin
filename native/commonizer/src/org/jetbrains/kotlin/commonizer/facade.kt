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
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

fun runCommonization(parameters: CommonizerParameters) {
    if (!parameters.hasAnythingToCommonize()) {
        parameters.resultsConsumer.allConsumed(Status.NOTHING_TO_DO)
        return
    }

    val storageManager = LockBasedStorageManager("Declarations commonization")

    val mergeResult = mergeAndCommonize(storageManager, parameters)
    val mergedTree = mergeResult.root

    // build resulting declarations:
    for (targetIndex in 0 until mergedTree.dimension) {
        serializeTarget(mergeResult, targetIndex, parameters)
    }

    parameters.resultsConsumer.allConsumed(Status.DONE)
}

private fun mergeAndCommonize(storageManager: StorageManager, parameters: CommonizerParameters): CirTreeMergeResult {
    // build merged tree:
    val classifiers = CirKnownClassifiers(
        commonizedNodes = CirCommonizedClassifierNodes.default(),
        commonDependencies = CirProvidedClassifiers.of(
            CirFictitiousFunctionClassifiers,
            CirProvidedClassifiers.by(parameters.commonDependencyModulesProvider)
        )
    )
    val mergeResult = mergeCirTree(storageManager, classifiers, parameters)

    // commonize:
    mergeResult.root.accept(CommonizationVisitor(classifiers, mergeResult.root), Unit)
    parameters.progressLogger?.invoke("Commonized declarations")

    return mergeResult
}

private fun serializeTarget(mergeResult: CirTreeMergeResult, targetIndex: Int, parameters: CommonizerParameters) {
    val mergedTree = mergeResult.root
    val target = mergedTree.getTarget(targetIndex)

    CirTreeSerializer.serializeSingleTarget(mergedTree, targetIndex, parameters.statsCollector) { metadataModule ->
        val libraryName = metadataModule.name
        val serializedMetadata = with(metadataModule.write(KLIB_FRAGMENT_WRITE_STRATEGY)) {
            SerializedMetadata(header, fragments, fragmentNames)
        }
        val manifestData = parameters.manifestDataProvider.getManifest(target, libraryName)
        parameters.resultsConsumer.consume(target, ModuleResult.Commonized(libraryName, serializedMetadata, manifestData))
    }

    if (target is LeafCommonizerTarget) {
        mergeResult.missingModuleInfos.getValue(target).forEach {
            parameters.resultsConsumer.consume(target, ModuleResult.Missing(it.originalLocation))
        }
    }

    parameters.resultsConsumer.targetConsumed(target)
}

private val KLIB_FRAGMENT_WRITE_STRATEGY = ChunkedKlibModuleFragmentWriteStrategy()
