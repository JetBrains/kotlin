/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree.mergers

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.commonizer.ModulesProvider
import org.jetbrains.kotlin.commonizer.TargetProvider
import org.jetbrains.kotlin.commonizer.cir.CirRoot
import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.commonizer.mergedtree.CirTargetMergingContext
import org.jetbrains.kotlin.commonizer.metadata.utils.SerializedMetadataLibraryProvider

internal class TargetMerger(
    private val moduleMerger: ModuleMerger
) {
    fun processTarget(
        context: CirTargetMergingContext,
        rootNode: CirRootNode,
        targetProvider: TargetProvider,
        commonModuleInfos: Collection<ModulesProvider.ModuleInfo>
    ): Unit = with(context) {
        rootNode.targetDeclarations[targetIndex] = CirRoot.create(targetProvider.target)

        if (commonModuleInfos.isEmpty()) {
            return
        }

        commonModuleInfos.forEach { moduleInfo ->
            val metadata = targetProvider.modulesProvider.loadModuleMetadata(moduleInfo.name)
            val module = KlibModuleMetadata.read(SerializedMetadataLibraryProvider(metadata))
            moduleMerger.processModule(context, rootNode, module)
        }
    }
}
