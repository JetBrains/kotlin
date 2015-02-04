/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.plugin.android

import org.jetbrains.kotlin.idea.caches.resolve.getModuleInfo
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.psi.PsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import com.intellij.psi.xml.XmlAttribute
import com.intellij.openapi.actionSystem.DataContext
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import com.intellij.openapi.module.ModuleServiceManager
import org.jetbrains.kotlin.lang.resolve.android.AndroidUIXmlProcessor

public class AndroidGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
        if (sourceElement is LeafPsiElement && sourceElement.getParent() is JetSimpleNameExpression) {
            val resolved = JetSimpleNameReference(sourceElement.getParent() as JetSimpleNameExpression).resolve()
            if (resolved == null) return null
            val name = if (resolved is JetProperty) {
                resolved.getName()
            }
            else null
            if (name != null) {
                val moduleInfo = sourceElement.getModuleInfo()
                if (moduleInfo !is ModuleSourceInfo) return null

                val parser = ModuleServiceManager.getService(moduleInfo.module, javaClass<AndroidUIXmlProcessor>())
                val psiElement = parser.resourceManager.idToXmlAttribute(name) as? XmlAttribute
                if (psiElement != null) {
                    return array(psiElement.getValueElement())
                }
            }
        }
        return null
    }

    override fun getActionText(context: DataContext?): String? {
        return null
    }

}