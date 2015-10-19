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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.android.dom.wrappers.LazyValueResourceElementWrapper
import org.jetbrains.android.util.AndroidResourceUtil
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.idToName
import org.jetbrains.kotlin.android.synthetic.isAndroidSyntheticElement
import org.jetbrains.kotlin.android.synthetic.nameToIdDeclaration
import org.jetbrains.kotlin.android.synthetic.res.SyntheticFileGenerator
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.getModuleInfo
import org.jetbrains.kotlin.psi.KtProperty

public class AndroidRenameProcessor : RenamePsiElementProcessor() {

    override fun canProcessElement(element: PsiElement): Boolean {
        // Either renaming synthetic property, or value in layout xml, or R class field
        return (element.namedUnwrappedElement is KtProperty &&
                isAndroidSyntheticElement(element.namedUnwrappedElement)) || element is XmlAttributeValue ||
               isRClassField(element)
    }

    private fun isRClassField(element: PsiElement): Boolean {
        return if (element is PsiField) {
            val outerClass = element.getParent()?.getParent()
            if (outerClass !is PsiClass) return false

            if (outerClass.getQualifiedName()?.startsWith(AndroidConst.SYNTHETIC_PACKAGE) ?: false)
                true else false
        }
        else false
    }


    private fun PsiElement.getModule(): Module? {
        val moduleInfo = getModuleInfo()
        return if (moduleInfo is ModuleSourceInfo) moduleInfo.module else null
    }

    override fun prepareRenaming(
            element: PsiElement?,
            newName: String,
            allRenames: MutableMap<PsiElement, String>,
            scope: SearchScope
    ) {
        if (element != null && element.namedUnwrappedElement is KtProperty) {
            renameSyntheticProperty(element.namedUnwrappedElement as KtProperty, newName, allRenames)
        }
        else if (element is XmlAttributeValue) {
            renameAttributeValue(element, newName, allRenames)
        }
        else if (element is LightElement) {
            renameLightClassField(element, newName, allRenames)
        }
    }

    private fun renameSyntheticProperty(
            jetProperty: KtProperty,
            newName: String,
            allRenames: MutableMap<PsiElement, String>
    ) {
        val module = jetProperty.getModule() ?: return

        val processor = ModuleServiceManager.getService(module, javaClass<SyntheticFileGenerator>())!!
        val resourceManager = processor.layoutXmlFileManager

        val psiElements = resourceManager.propertyToXmlAttributes(jetProperty).map { it as? XmlAttribute }.filterNotNull()

        for (psiElement in psiElements) {
            val valueElement = psiElement.getValueElement()

            if (valueElement != null) {
                allRenames[XmlAttributeValueWrapper(valueElement)] = nameToIdDeclaration(newName)
                val name = AndroidResourceUtil.getResourceNameByReferenceText(newName) ?: return
                for (resField in AndroidResourceUtil.findIdFields(psiElement)) {
                    allRenames.put(resField, AndroidResourceUtil.getFieldNameByResourceName(name))
                }
            }
        }
    }

    private fun renameAttributeValue(
            attribute: XmlAttributeValue,
            newName: String,
            allRenames: MutableMap<PsiElement, String>
    ) {
        val element = LazyValueResourceElementWrapper.computeLazyElement(attribute)
        val module = attribute.getModule() ?: ModuleUtilCore.findModuleForFile(
                attribute.getContainingFile().getVirtualFile(), attribute.getProject()) ?: return

        val processor = ModuleServiceManager.getService(module, javaClass<SyntheticFileGenerator>())!!
        if (element == null) return
        val oldPropName = AndroidResourceUtil.getResourceNameByReferenceText(attribute.getValue())
        val newPropName = idToName(newName)
        if (oldPropName != null && newPropName != null) {
            renameSyntheticProperties(allRenames, newPropName, oldPropName, processor)
        }
    }

    private fun renameSyntheticProperties(
            allRenames: MutableMap<PsiElement, String>,
            newPropName: String,
            oldPropName: String,
            processor: SyntheticFileGenerator
    ) {
        val props = processor.getSyntheticFiles()?.flatMap { it.findChildrenByClass(javaClass<KtProperty>()).toList() }
        val matchedProps = props?.filter { it.getName() == oldPropName } ?: listOf()
        for (prop in matchedProps) {
            allRenames[prop] = newPropName
        }
    }

    private fun renameLightClassField(
            field: LightElement,
            newName: String,
            allRenames: MutableMap<PsiElement, String>
    ) {
        val oldName = field.getName()!!
        val processor = ServiceManager.getService(field.getProject(), javaClass<SyntheticFileGenerator>())
        renameSyntheticProperties(allRenames, newName, oldName, processor)
    }

}
