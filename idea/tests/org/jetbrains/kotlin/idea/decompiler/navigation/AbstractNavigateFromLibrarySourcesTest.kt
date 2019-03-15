/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.utils.sure

abstract class AbstractNavigateFromLibrarySourcesTest : LightCodeInsightFixtureTestCase() {
    protected fun navigationElementForReferenceInLibrarySource(filePath: String, referenceText: String): PsiElement {
        val libraryOrderEntry = ModuleRootManager.getInstance(myModule!!).orderEntries.first { it is LibraryOrderEntry }
        val libSourcesRoot = libraryOrderEntry.getUrls(OrderRootType.SOURCES)[0]
        val libUrl = "$libSourcesRoot/$filePath"
        val vf = VirtualFileManager.getInstance().refreshAndFindFileByUrl(libUrl)
            ?: error("Can't find library: $libUrl")
        val psiFile = psiManager.findFile(vf)!!
        val indexOf = psiFile.text!!.indexOf(referenceText)
        val reference = psiFile.findReferenceAt(indexOf)
        return reference.sure { "Couldn't find reference" }.resolve().sure { "Couldn't resolve reference" }.navigationElement!!
    }
}
