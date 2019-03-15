/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.slicer.SliceRootNode
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractSlicerLeafGroupingTest : AbstractSlicerTest() {
    override fun doTest(path: String, sliceProvider: KotlinSliceProvider, rootNode: SliceRootNode) {
        val treeStructure = SliceTreeStructure(rootNode)
        val analyzer = sliceProvider.leafAnalyzer
        val possibleElementsByNode = analyzer.createMap()
        val leafExpressions = analyzer.calcLeafExpressions(rootNode, treeStructure, possibleElementsByNode)
        val newRootNode = analyzer.createTreeGroupedByValues(leafExpressions, rootNode, possibleElementsByNode)
        val renderedForest = buildString {
            for (groupRootNode in newRootNode.children) {
                append(buildTreeRepresentation(groupRootNode))
                append("\n")
            }
        }
        KotlinTestUtils.assertEqualsToFile(File(path.replace(".kt", ".leafGroups.txt")), renderedForest)
    }
}