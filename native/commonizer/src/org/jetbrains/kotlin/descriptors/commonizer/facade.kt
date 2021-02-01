/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import kotlinx.metadata.klib.ChunkedKlibModuleFragmentWriteStrategy
import org.jetbrains.kotlin.descriptors.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.descriptors.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.descriptors.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirTreeMerger.CirTreeMergeResult
import org.jetbrains.kotlin.descriptors.commonizer.metadata.MetadataBuilder
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

fun runCommonization(parameters: CommonizerParameters) {
    val resultsConsumer = parameters.resultsConsumer
    if (!parameters.hasAnythingToCommonize()) {
        resultsConsumer.successfullyFinished(Status.NOTHING_TO_DO)
        return
    }

    val storageManager = LockBasedStorageManager("Declarations commonization")

    val mergeResult = mergeAndCommonize(storageManager, parameters)
    val mergedTree = mergeResult.root

    // build resulting declarations:
    val klibFragmentWriteStrategy = ChunkedKlibModuleFragmentWriteStrategy()

    for (targetIndex in 0 until mergedTree.dimension) {
        val (target, metadataModules) = MetadataBuilder.build(mergedTree, targetIndex, parameters.statsCollector)

        val commonizedModules: List<ModuleResult.Commonized> = metadataModules.map { metadataModule ->
            val libraryName = metadataModule.name
            val serializedMetadata = with(metadataModule.write(klibFragmentWriteStrategy)) {
                SerializedMetadata(header, fragments, fragmentNames)
            }

            ModuleResult.Commonized(libraryName, serializedMetadata)
        }

        val missingModules: List<ModuleResult.Missing> = if (target is LeafTarget)
            mergeResult.missingModuleInfos.getValue(target).map { ModuleResult.Missing(it.originalLocation) }
        else emptyList()

        resultsConsumer.consumeResults(target, commonizedModules + missingModules)
    }

    resultsConsumer.successfullyFinished(Status.DONE)
}

private fun mergeAndCommonize(storageManager: StorageManager, parameters: CommonizerParameters): CirTreeMergeResult {
    // build merged tree:
    val classifiers = CirKnownClassifiers(
        commonized = CirCommonizedClassifiers.default(),
        forwardDeclarations = CirForwardDeclarations.default(),
        dependeeLibraries = mapOf(
            // for now, supply only common dependee libraries (ex: Kotlin stdlib)
            parameters.sharedTarget to CirProvidedClassifiers.fromModules(storageManager) {
                parameters.dependeeModulesProvider?.loadModules(emptyList())?.values.orEmpty()
            }
        )
    )
    val mergeResult = CirTreeMerger(storageManager, classifiers, parameters).merge()

    // commonize:
    val mergedTree = mergeResult.root
    mergedTree.accept(CommonizationVisitor(classifiers, mergedTree), Unit)
    parameters.progressLogger?.invoke("Commonized declarations")

    return mergeResult
}
