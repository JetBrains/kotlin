/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.editor.quickDoc

import com.intellij.codeInsight.CodeInsightTestCase
import com.intellij.ide.hierarchy.HierarchyBrowserBaseEx
import com.intellij.ide.hierarchy.LanguageTypeHierarchy
import com.intellij.ide.hierarchy.type.TypeHierarchyNodeDescriptor
import com.intellij.ide.hierarchy.type.TypeHierarchyTreeStructure
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.testFramework.MapDataContext
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.idea.KotlinQuickDocumentationProvider
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

public class QuickDocInHierarchyTest() : CodeInsightTestCase() {
    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/kdoc/inTypeHierarchy/"
    }

    public fun testSimple() {
        configureByFile(getTestName(true) + ".kt")

        val context = MapDataContext()
        context.put<Project>(CommonDataKeys.PROJECT, getProject())
        context.put<Editor>(CommonDataKeys.EDITOR, getEditor())

        val hierarchyTreeStructure = TypeHierarchyTreeStructure(
                getProject(),
                LanguageTypeHierarchy.INSTANCE.forLanguage(JetLanguage.INSTANCE).getTarget(context) as PsiClass?,
                HierarchyBrowserBaseEx.SCOPE_PROJECT
        )
        val hierarchyNodeDescriptor = hierarchyTreeStructure.getBaseDescriptor() as TypeHierarchyNodeDescriptor
        val doc = KotlinQuickDocumentationProvider().generateDoc(hierarchyNodeDescriptor.getPsiClass() as PsiElement, null)!!

        TestCase.assertTrue("Invalid doc\n: $doc", doc.contains("Very special class"))
    }
}