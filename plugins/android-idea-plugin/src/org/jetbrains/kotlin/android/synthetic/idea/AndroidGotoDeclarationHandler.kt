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
import org.jetbrains.kotlin.android.synthetic.res.SyntheticFileGenerator
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.getModuleInfo
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetSimpleNameExpression

public class AndroidGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        if (sourceElement is LeafPsiElement && sourceElement.getParent() is JetSimpleNameExpression) {
            val resolved = JetSimpleNameReference(sourceElement.getParent() as JetSimpleNameExpression).resolve()
            val property = resolved as? JetProperty ?: return null

            val moduleInfo = sourceElement.getModuleInfo()
            if (moduleInfo !is ModuleSourceInfo) return null

            val parser = ModuleServiceManager.getService(moduleInfo.module, javaClass<SyntheticFileGenerator>())!!
            val psiElements = parser.layoutXmlFileManager.propertyToXmlAttributes(property)
            val valueElements = psiElements.map { (it as? XmlAttribute)?.getValueElement() as? PsiElement }.filterNotNull()
            if (valueElements.isNotEmpty()) return valueElements.toTypedArray()
        }
        return null
    }

    override fun getActionText(context: DataContext?): String? {
        return null
    }

}