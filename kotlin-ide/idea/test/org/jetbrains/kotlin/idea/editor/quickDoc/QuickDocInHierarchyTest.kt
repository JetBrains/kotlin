/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor.quickDoc

import com.intellij.codeInsight.CodeInsightTestCase
import com.intellij.ide.hierarchy.HierarchyBrowserBaseEx
import com.intellij.ide.hierarchy.LanguageTypeHierarchy
import com.intellij.ide.hierarchy.actions.BrowseHierarchyActionBase
import com.intellij.ide.hierarchy.type.TypeHierarchyNodeDescriptor
import com.intellij.ide.hierarchy.type.TypeHierarchyTreeStructure
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.testFramework.MapDataContext
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinDocumentationProvider
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.test.util.slashedPath
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
class QuickDocInHierarchyTest : CodeInsightTestCase() {
    override fun getTestDataPath() = IDEA_TEST_DATA_DIR.resolve("kdoc/inTypeHierarchy").slashedPath

    fun testSimple() {
        configureByFile(getTestName(true) + ".kt")

        val context = MapDataContext()
        context.put(CommonDataKeys.PROJECT, project)
        context.put(CommonDataKeys.EDITOR, editor)

        val provider = BrowseHierarchyActionBase.findProvider(LanguageTypeHierarchy.INSTANCE, file, file, context)!!
        val hierarchyTreeStructure = TypeHierarchyTreeStructure(
            project,
            provider.getTarget(context) as PsiClass,
            HierarchyBrowserBaseEx.SCOPE_PROJECT
        )

        val hierarchyNodeDescriptor = hierarchyTreeStructure.baseDescriptor as TypeHierarchyNodeDescriptor
        val doc = KotlinDocumentationProvider().generateDoc(hierarchyNodeDescriptor.psiClass as PsiElement, null)!!

        TestCase.assertTrue("Invalid doc\n: $doc", doc.contains("Very special class"))
    }
}