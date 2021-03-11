/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.hierarchy

import com.intellij.ide.hierarchy.type.JavaTypeHierarchyProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch

class KotlinTypeHierarchyProviderBySuperTypeCallEntry : JavaTypeHierarchyProvider() {
    override fun getTarget(dataContext: DataContext): PsiElement? {
        val project = PlatformDataKeys.PROJECT.getData(dataContext) ?: return null
        val editor = PlatformDataKeys.EDITOR.getData(dataContext) ?: return null

        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null

        val offset = editor.caretModel.offset
        val elementAtCaret = file.findElementAt(offset) ?: return null
        if (elementAtCaret.getParentOfTypeAndBranch<KtSuperTypeCallEntry> { calleeExpression } == null) return null

        val targetElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext)?.unwrapped
        return when {
            targetElement is KtConstructor<*> -> targetElement.containingClassOrObject?.toLightClass()
            targetElement is PsiMethod && targetElement.isConstructor -> targetElement.containingClass
            targetElement is KtClassOrObject -> targetElement.toLightClass()
            targetElement is PsiClass -> targetElement
            else -> null
        }
    }
}
