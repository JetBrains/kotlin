/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

abstract class AbstractNavigateFromLibrarySourcesTest : LightJavaCodeInsightFixtureTestCase() {
    protected fun navigationElementForReferenceInLibrarySource(filePath: String, referenceText: String): PsiElement {
        val libraryOrderEntry = ModuleRootManager.getInstance(module).orderEntries.first { it is LibraryOrderEntry }
        val libSourcesRoot = libraryOrderEntry.getUrls(OrderRootType.SOURCES)[0]
        val libUrl = "$libSourcesRoot/$filePath"
        val virtualFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl(libUrl) ?: error("Can't find library: $libUrl")
        val psiFile = psiManager.findFile(virtualFile) ?: error("Can't find PSI file for $virtualFile")
        val indexOf = psiFile.text.indexOf(referenceText)
        val reference = psiFile.findReferenceAt(indexOf) ?: error("Can't find reference at index $indexOf")
        val resolvedElement = reference.resolve() ?: error("Can't resolve reference")
        return resolvedElement.navigationElement ?: error("Can't get navigation element")
    }
}
