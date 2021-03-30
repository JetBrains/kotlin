/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree.mergers

import kotlinx.metadata.KmModuleFragment
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.commonizer.cir.CirModule
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.commonizer.mergedtree.CirModuleNode
import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.commonizer.mergedtree.CirTargetMergingContext
import org.jetbrains.kotlin.commonizer.mergedtree.buildModuleNode
import org.jetbrains.kotlin.commonizer.utils.foldToMap

internal class ModuleMerger(
    private val packageMerger: PackageMerger
) {
    fun processModule(
        context: CirTargetMergingContext,
        rootNode: CirRootNode,
        module: KlibModuleMetadata
    ): Unit = with(context) {
        val moduleName: CirName = CirName.create(module.name)
        val moduleNode: CirModuleNode = rootNode.modules.getOrPut(moduleName) {
            buildModuleNode(storageManager, targets)
        }
        moduleNode.targetDeclarations[context.targetIndex] = CirModule.create(moduleName)

        val groupedFragments: Map<CirPackageName, Collection<KmModuleFragment>> = module.fragments.foldToMap { fragment ->
            fragment.fqName?.let(CirPackageName.Companion::create) ?: error("A fragment without FQ name in module $moduleName: $fragment")
        }

        groupedFragments.forEach { (packageName, fragments) ->
            packageMerger.processFragments(context, moduleNode, fragments, packageName)
        }
    }
}
