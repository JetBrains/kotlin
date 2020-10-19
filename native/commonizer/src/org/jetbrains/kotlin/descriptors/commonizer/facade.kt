/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import kotlinx.metadata.klib.ChunkedKlibModuleFragmentWriteStrategy
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor1
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor2
import org.jetbrains.kotlin.descriptors.commonizer.builder.createGlobalBuilderComponents
import org.jetbrains.kotlin.descriptors.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.dimension
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirTreeMerger.CirTreeMergeResult
import org.jetbrains.kotlin.descriptors.commonizer.metadata.MetadataBuilder
import org.jetbrains.kotlin.descriptors.commonizer.utils.strip
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.serialization.konan.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

fun runCommonization(parameters: CommonizerParameters): CommonizerResult {
    if (!parameters.hasAnythingToCommonize())
        return CommonizerResult.NothingToDo

    val storageManager = LockBasedStorageManager("Declaration descriptors commonization")

    val mergeResult = mergeAndCommonize(storageManager, parameters)
    val mergedTree = mergeResult.root

    // build resulting descriptors:
    val modulesByTargets = LinkedHashMap<CommonizerTarget, Collection<ModuleResult>>() // use linked hash map to preserve order
    val klibFragmentWriteStrategy = ChunkedKlibModuleFragmentWriteStrategy()

    // optional part for generating descriptors: begin
    val components = mergedTree.createGlobalBuilderComponents(storageManager, parameters)
    if (components != null) {
        mergedTree.accept(DeclarationsBuilderVisitor1(components), emptyList())
        mergedTree.accept(DeclarationsBuilderVisitor2(components), emptyList())
    }
    // optional part for generating descriptors: end

    for (targetIndex in 0 until mergedTree.dimension) {
        val (target, metadataModules) = MetadataBuilder.build(mergedTree, targetIndex)

        // optional part for generating descriptors: begin
        val moduleDescriptors: Map<String, ModuleDescriptor>? = components?.targetComponents?.get(targetIndex)?.let { component ->
            check(component.target == target)
            check(component.index == targetIndex)

            components.cache.getAllModules(targetIndex)
                .filter { it.name != FORWARD_DECLARATIONS_MODULE_NAME }
                .associateBy { it.name.strip() }
        }
        // optional part for generating descriptors: end

        val commonizedModules: List<ModuleResult.Commonized> = metadataModules.map { metadataModule ->
            val libraryName = metadataModule.name
            val serializedMetadata = with(metadataModule.write(klibFragmentWriteStrategy)) {
                SerializedMetadata(header, fragments, fragmentNames)
            }

            val libraryMetadata = LibraryMetadata(libraryName, serializedMetadata)

            // optional part for generating descriptors: begin
            val moduleDescriptor = moduleDescriptors?.get(libraryName)
            // optional part for generating descriptors: end

            ModuleResult.Commonized(moduleDescriptor, libraryMetadata)
        }
        parameters.progressLogger?.invoke("Built metadata for target [$target]")

        val missingModules: List<ModuleResult.Missing> = if (target is LeafTarget)
            mergeResult.missingModuleInfos.getValue(target).map { ModuleResult.Missing(it.originalLocation) }
        else emptyList()

        modulesByTargets[target] = commonizedModules + missingModules
    }

    return CommonizerResult.Done(modulesByTargets)
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
