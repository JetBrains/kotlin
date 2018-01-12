/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.test.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.utils.sure
import kotlin.test.assertTrue

abstract class AbstractNavigateFromLibrarySourcesTest : LightCodeInsightFixtureTestCase() {
    protected fun navigationElementForReferenceInLibrarySource(filePath: String, referenceText: String): PsiElement {
        val libraryOrderEntry = ModuleRootManager.getInstance(myModule!!).orderEntries.first { it is LibraryOrderEntry }
        val libSourcesRoot = libraryOrderEntry.getUrls(OrderRootType.SOURCES)[0]
        val vf = VirtualFileManager.getInstance().findFileByUrl(libSourcesRoot + "/$filePath")!!
        val psiFile = psiManager.findFile(vf)!!
        val indexOf = psiFile.text!!.indexOf(referenceText)
        val reference = psiFile.findReferenceAt(indexOf)
        return reference.sure { "Couldn't find reference" }.resolve().sure { "Couldn't resolve reference" }.navigationElement!!
    }
}
