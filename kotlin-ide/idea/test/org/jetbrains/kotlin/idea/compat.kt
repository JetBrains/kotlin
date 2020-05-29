/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Computable
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.codeInsight.hierarchy.HierarchyViewTestFixture

// FIX ME WHEN BUNCH 193 REMOVED
@Suppress("UNUSED_PARAMETER")
fun doHierarchyTestCompat(
    hierarchyFixture: HierarchyViewTestFixture,
    treeStructureComputable: Computable<out HierarchyTreeStructure>,
    expectedStructure: String,
) {
    HierarchyViewTestFixture.doHierarchyTest(treeStructureComputable.compute(), expectedStructure)
}

fun <T> maskExtensions(
    pointName: ExtensionPointName<T>,
    newExtensions: List<T>,
    parentDisposable: Disposable
) {
    ExtensionTestUtil.maskExtensions(pointName, newExtensions, parentDisposable)
}
