/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlinx.metadata.klib.ChunkedKlibModuleFragmentWriteStrategy
import org.jetbrains.kotlin.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.commonizer.mergedtree.CirCommonizedClassifierNodes
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.commonizer.metadata.CirTreeSerializer
import org.jetbrains.kotlin.commonizer.transformer.Checked.Companion.invoke
import org.jetbrains.kotlin.commonizer.transformer.InlineTypeAliasCirNodeTransformer
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot
import org.jetbrains.kotlin.commonizer.tree.defaultCirTreeRootDeserializer
import org.jetbrains.kotlin.commonizer.tree.mergeCirTree
import org.jetbrains.kotlin.commonizer.utils.progress
import org.jetbrains.kotlin.library.SerializedMetadata

fun runCommonization(parameters: CommonizerParameters) {
    if (!parameters.containsCommonModuleNames()) {
        parameters.resultsConsumer.allConsumed(parameters, Status.NOTHING_TO_DO)
        return
    }
    CommonizerQueue(parameters).invokeAll()
    parameters.resultsConsumer.allConsumed(parameters, Status.DONE)
}

internal fun deserializeTarget(parameters: CommonizerParameters, target: TargetProvider): CirTreeRoot {
    return parameters.logger.progress(target.target, "Deserialized declarations") {
        defaultCirTreeRootDeserializer(parameters, target)
    }
}

internal fun deserializeTarget(parameters: CommonizerParameters, target: CommonizerTarget): CirTreeRoot? {
    val targetProvider = parameters.targetProviders[target] ?: return null
    return deserializeTarget(parameters, targetProvider)
}

internal fun commonizeTarget(
    parameters: CommonizerParameters,
    inputs: TargetDependent<CirTreeRoot?>,
    output: CommonizerTarget
): CirRootNode? {
    parameters.logger.progress(output, "Commonized declarations from ${inputs.targets}") {
        val availableTrees = inputs.filterNonNull()
        /* Nothing to merge */
        if (availableTrees.size == 0) return null

        val classifiers = CirKnownClassifiers(
            commonizedNodes = CirCommonizedClassifierNodes.default(),
            commonDependencies = parameters.dependencyClassifiers(output)
        )

        val mergedTree = mergeCirTree(parameters.storageManager, classifiers, availableTrees)
        InlineTypeAliasCirNodeTransformer(parameters.storageManager, classifiers).invoke(mergedTree)
        mergedTree.accept(CommonizationVisitor(classifiers, mergedTree), Unit)

        return mergedTree
    }
}

internal fun serializeTarget(
    parameters: CommonizerParameters,
    commonized: CirRootNode,
    outputTarget: SharedCommonizerTarget
): Unit = parameters.logger.progress(outputTarget, "Serialized target") {
    CirTreeSerializer.serializeSingleTarget(commonized, commonized.indexOfCommon, parameters.statsCollector) { metadataModule ->
        val libraryName = metadataModule.name
        val serializedMetadata = with(metadataModule.write(ChunkedKlibModuleFragmentWriteStrategy())) {
            SerializedMetadata(header, fragments, fragmentNames)
        }
        val manifestData = parameters.manifestProvider[outputTarget].buildManifest(libraryName)
        parameters.resultsConsumer.consume(
            parameters, outputTarget,
            ResultsConsumer.ModuleResult.Commonized(libraryName, serializedMetadata, manifestData)
        )
    }
    parameters.resultsConsumer.targetConsumed(parameters, outputTarget)
}
