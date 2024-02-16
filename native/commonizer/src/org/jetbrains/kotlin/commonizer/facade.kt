/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlinx.metadata.klib.ChunkedKlibModuleFragmentWriteStrategy
import org.jetbrains.kotlin.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirCommonizedClassifierNodes
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.commonizer.metadata.CirTreeSerializer
import org.jetbrains.kotlin.commonizer.transformer.InlineTypeAliasCirNodeTransformer
import org.jetbrains.kotlin.commonizer.transformer.ReApproximationCirNodeTransformer
import org.jetbrains.kotlin.commonizer.transformer.ReApproximationCirNodeTransformer.SignatureBuildingContextProvider
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
    val availableTrees = inputs.filterNonNull()
    /* Nothing to merge */
    if (availableTrees.size == 0) return null

    parameters.logger.progress(output, "Commonized declarations from ${inputs.targets}") {
        val classifiers = CirKnownClassifiers(
            classifierIndices = availableTrees.mapValue(::CirClassifierIndex),
            targetDependencies = availableTrees.mapValue(CirTreeRoot::dependencies),
            commonizedNodes = CirCommonizedClassifierNodes.default(allowedDuplicates = allowedDuplicates),
            commonDependencies = parameters.dependencyClassifiers(output)
        )

        val mergedTree = mergeCirTree(parameters.storageManager, classifiers, availableTrees, parameters.settings)

        InlineTypeAliasCirNodeTransformer(parameters.storageManager, classifiers, parameters.settings).invoke(mergedTree)

        ReApproximationCirNodeTransformer(
            parameters.storageManager, classifiers, parameters.settings,
            SignatureBuildingContextProvider(classifiers, typeAliasInvariant = true, skipArguments = false)
        ).invoke(mergedTree)

        ReApproximationCirNodeTransformer(
            parameters.storageManager, classifiers, parameters.settings,
            SignatureBuildingContextProvider(classifiers, typeAliasInvariant = true, skipArguments = true)
        ).invoke(mergedTree)

        mergedTree.accept(CommonizationVisitor(mergedTree), Unit)

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
            ResultsConsumer.ModuleResult(libraryName, serializedMetadata, manifestData)
        )
    }
    parameters.resultsConsumer.targetConsumed(parameters, outputTarget)
}

// iOS, tvOS, watchOS SDKs from Xcode 14 moved some declarations from CoreGraphics to CoreFoundation (CFCGTypes.h).
// Unfortunately (1), for Kotlin/Native cinterop this is a breaking change, so CFCGTypes platform library with `platform.CoreGraphics`
// package was introduced to mitigate this problem.
// Unfortunately (2), macOS SDK does not have this change and commonizer fails on commonizing (macOS, *OS),
// because it was written in assumption that different platform klibs contain different packages.
// We workaround this problem by allowing some classifiers to clash in `CirCommonizedClassifierNodes`.
// Fortunately (1), macOS SDK from Xcode 14.1 should have CFCGTypes.h, so this hack can be removed soon.
// TODO: Remove hack after Xcode 14.1.
private val allowedDuplicates = run {
    val cfcgTypesClassifiers = setOf(
        "platform/CoreGraphics/CGAffineTransform",
        "platform/CoreGraphics/CGAffineTransform.Companion",
        "platform/CoreGraphics/CGFloat",
        "platform/CoreGraphics/CGFloatVar",
        "platform/CoreGraphics/CGPoint",
        "platform/CoreGraphics/CGPoint.Companion",
        "platform/CoreGraphics/CGRect",
        "platform/CoreGraphics/CGRect.Companion",
        "platform/CoreGraphics/CGRectEdge",
        "platform/CoreGraphics/CGRectEdge.CGRectMaxXEdge",
        "platform/CoreGraphics/CGRectEdge.CGRectMaxYEdge",
        "platform/CoreGraphics/CGRectEdge.CGRectMinXEdge",
        "platform/CoreGraphics/CGRectEdge.CGRectMinYEdge",
        "platform/CoreGraphics/CGRectEdge.Companion",
        "platform/CoreGraphics/CGRectEdge.Var",
        "platform/CoreGraphics/CGRectEdge.Var.Companion",
        "platform/CoreGraphics/CGSize",
        "platform/CoreGraphics/CGSize.Companion",
        "platform/CoreGraphics/CGVector",
        "platform/CoreGraphics/CGVector.Companion",
    )
    cfcgTypesClassifiers.map { CirEntityId.create(it) }.toSet()
}
