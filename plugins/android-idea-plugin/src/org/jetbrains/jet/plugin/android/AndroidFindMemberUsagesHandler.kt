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

package org.jetbrains.jet.plugin.android

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import org.jetbrains.jet.lang.resolve.android.isAndroidSyntheticElement
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlProcessor
import com.intellij.psi.xml.XmlAttribute
import org.jetbrains.android.util.AndroidResourceUtil
import java.util.ArrayList
import com.intellij.find.findUsages.JavaVariableFindUsagesOptions
import org.jetbrains.jet.plugin.findUsages.handlers.KotlinFindUsagesHandlerDecorator
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.idea.util.application.runReadAction

class AndroidFindUsageHandlerDecorator : KotlinFindUsagesHandlerDecorator {
    override fun decorateHandler(element: PsiElement, forHighlightUsages: Boolean, delegate: FindUsagesHandler): FindUsagesHandler {
        if (element !is JetNamedDeclaration) return delegate
        if (!isAndroidSyntheticElement(element)) return delegate

        return AndroidFindMemberUsagesHandler(element, delegate)
    }
}

class AndroidFindMemberUsagesHandler(
        private val declaration: JetNamedDeclaration,
        private val delegate: FindUsagesHandler? = null
) : FindUsagesHandler(declaration) {

    override fun getPrimaryElements(): Array<PsiElement> {
        assert(isAndroidSyntheticElement(declaration))

        val name = (declaration as JetProperty).getName()!!
        val parser = ServiceManager.getService(declaration.getProject(), javaClass<AndroidUIXmlProcessor>())
        val psiElement = parser?.resourceManager?.idToXmlAttribute(name) as? XmlAttribute
        if (psiElement != null && psiElement.getValueElement() != null) {
            return array(psiElement.getValueElement()!!)
        }

        return super.getPrimaryElements()
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return delegate?.getFindUsagesOptions(dataContext) ?: super.getFindUsagesOptions(dataContext)
    }

    override fun getSecondaryElements(): Array<PsiElement> {
        assert(isAndroidSyntheticElement(declaration))

        val name = (declaration as JetProperty).getName()!!
        val parser = ServiceManager.getService(declaration.getProject(), javaClass<AndroidUIXmlProcessor>())
        val psiElement = parser?.resourceManager?.idToXmlAttribute(name) as? XmlAttribute
        if (psiElement != null) {
            val res = ArrayList<PsiElement>()
            val fields = AndroidResourceUtil.findIdFields(psiElement)
            for (field in fields) {
                res.add(field)
            }
            res.add(declaration)
            return res.copyToArray()
        }

        return super.getSecondaryElements()
    }

    override fun processElementUsages(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        assert(isAndroidSyntheticElement(declaration))

        val findUsagesOptions = JavaVariableFindUsagesOptions(runReadAction { element.getProject() })
        findUsagesOptions.isSearchForTextOccurrences = false
        findUsagesOptions.isSkipImportStatements = true
        findUsagesOptions.isUsages = true
        findUsagesOptions.isReadAccess = true
        findUsagesOptions.isWriteAccess = true
        return super.processElementUsages(element, processor, findUsagesOptions)
    }
}
