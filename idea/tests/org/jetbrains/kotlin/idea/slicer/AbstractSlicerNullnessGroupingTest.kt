/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.slicer.SliceRootNode
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractSlicerNullnessGroupingTest : AbstractSlicerTest() {
    override fun doTest(path: String, sliceProvider: KotlinSliceProvider, rootNode: SliceRootNode) {
        val treeStructure = TestSliceTreeStructure(rootNode)
        val analyzer = sliceProvider.nullnessAnalyzer
        val nullnessByNode = HackedSliceNullnessAnalyzerBase.createMap()
        val nullness = analyzer.calcNullableLeaves(rootNode, treeStructure, nullnessByNode)
        val newRootNode = analyzer.createNewTree(nullness, rootNode, nullnessByNode)
        val renderedForest = buildString {
            for (groupRootNode in newRootNode.children) {
                append(buildTreeRepresentation(groupRootNode))
                append("\n")
            }
        }
        KotlinTestUtils.assertEqualsToFile(File(path.replace(".kt", ".nullnessGroups.txt")), renderedForest)
    }
}