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

package org.jetbrains.kotlin.android.synthetic.idea

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.xml.XmlAttribute
import org.jetbrains.kotlin.android.synthetic.res.AndroidLayoutXmlFileManager
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getModuleInfo
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class AndroidGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        if (sourceElement is LeafPsiElement && sourceElement.parent is KtSimpleNameExpression) {
            val simpleNameExpression = sourceElement.parent as? KtSimpleNameExpression ?: return null
            val layoutManager = getLayoutManager(sourceElement) ?: return null
            val propertyDescriptor = resolvePropertyDescriptor(simpleNameExpression) ?: return null

            val psiElements = layoutManager.propertyToXmlAttributes(propertyDescriptor)
            val valueElements = psiElements.map { (it as? XmlAttribute)?.valueElement as? PsiElement }.filterNotNull()
            if (valueElements.isNotEmpty()) return valueElements.toTypedArray()
        }

        return null
    }

    private fun resolvePropertyDescriptor(simpleNameExpression: KtSimpleNameExpression): PropertyDescriptor? {
        val bindingContext = simpleNameExpression.analyze(BodyResolveMode.PARTIAL)
        val call = bindingContext[BindingContext.CALL, simpleNameExpression]
        val resolvedCall = bindingContext[BindingContext.RESOLVED_CALL, call]
        return resolvedCall?.resultingDescriptor as? PropertyDescriptor
    }

    private fun getLayoutManager(sourceElement: PsiElement): AndroidLayoutXmlFileManager? {
        val moduleInfo = sourceElement.getModuleInfo()
        if (moduleInfo !is ModuleSourceInfo) return null
        return ModuleServiceManager.getService(moduleInfo.module, AndroidLayoutXmlFileManager::class.java)
    }

    override fun getActionText(context: DataContext?): String? {
        return null
    }

}