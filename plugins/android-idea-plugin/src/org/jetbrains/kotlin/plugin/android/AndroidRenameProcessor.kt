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

package org.jetbrains.kotlin.plugin.android

import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.caches.resolve.getModuleInfo
import org.jetbrains.kotlin.psi.moduleInfo
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetProperty
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.PsiField
import com.intellij.psi.PsiClass
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.psi.search.SearchScope
import com.intellij.psi.impl.light.LightElement
import org.jetbrains.kotlin.lang.resolve.android.AndroidUIXmlProcessor
import com.intellij.psi.xml.XmlAttribute
import org.jetbrains.kotlin.lang.resolve.android
import org.jetbrains.kotlin.lang.resolve.android.AndroidConst
import com.intellij.openapi.module.ModuleServiceManager
import org.jetbrains.android.util.AndroidResourceUtil
import org.jetbrains.android.dom.wrappers.LazyValueResourceElementWrapper
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.lang.resolve.android.isAndroidSyntheticElement
import org.jetbrains.kotlin.lang.resolve.android.nameToIdDeclaration
import org.jetbrains.kotlin.lang.resolve.android.idToName

public class AndroidRenameProcessor : RenamePsiElementProcessor() {

    override fun canProcessElement(element: PsiElement): Boolean {
        // Either renaming synthetic property, or value in layout xml, or R class field
        return (element.namedUnwrappedElement is JetProperty &&
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
        if (moduleInfo is ModuleSourceInfo) return moduleInfo.module

        val file = getContainingFile() as? JetFile
        if (file != null) {
            val moduleInfo = file.moduleInfo
            if (moduleInfo is ModuleSourceInfo) return moduleInfo.module
        }

        return null
    }

    override fun prepareRenaming(
            element: PsiElement?,
            newName: String,
            allRenames: MutableMap<PsiElement, String>,
            scope: SearchScope
    ) {
        if (element != null && element.namedUnwrappedElement is JetProperty) {
            renameSyntheticProperty(element.namedUnwrappedElement as JetProperty, newName, allRenames, scope)
        }
        else if (element is XmlAttributeValue) {
            renameAttributeValue(element, newName, allRenames, scope)
        }
        else if (element is LightElement) {
            renameLightClassField(element, newName, allRenames, scope)
        }
    }

    private fun renameSyntheticProperty(
            jetProperty: JetProperty,
            newName: String,
            allRenames: MutableMap<PsiElement, String>,
            scope: SearchScope
    ) {
        val oldName = jetProperty.getName()
        val module = jetProperty.getModule()
        if (module == null) return

        val processor = ModuleServiceManager.getService(module, javaClass<AndroidUIXmlProcessor>())
        val resourceManager = processor.resourceManager
        val attr = resourceManager.idToXmlAttribute(oldName) as XmlAttribute
        allRenames[XmlAttributeValueWrapper(attr.getValueElement())] = nameToIdDeclaration(newName)
        val name = AndroidResourceUtil.getResourceNameByReferenceText(newName)
        for (resField in AndroidResourceUtil.findIdFields(attr)) {
            allRenames.put(resField, AndroidResourceUtil.getFieldNameByResourceName(name))
        }
    }

    private fun renameAttributeValue(
            attribute: XmlAttributeValue,
            newName: String,
            allRenames: MutableMap<PsiElement, String>,
            scope: SearchScope
    ) {
        val element = LazyValueResourceElementWrapper.computeLazyElement(attribute)
        val module = attribute.getModule() ?: ModuleUtilCore.findModuleForFile(
                attribute.getContainingFile().getVirtualFile(), attribute.getProject())
        if (module == null) return

        val processor = ModuleServiceManager.getService(module, javaClass<AndroidUIXmlProcessor>())
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
            processor: AndroidUIXmlProcessor
    ) {
        val props = processor.parseToPsi()?.flatMap { it.findChildrenByClass(javaClass<JetProperty>()).toList() }
        val matchedProps = props?.filter { it.getName() == oldPropName } ?: listOf()
        for (prop in matchedProps) {
            allRenames[prop] = newPropName
        }
    }

    private fun renameLightClassField(
            field: LightElement,
            newName: String,
            allRenames: MutableMap<PsiElement, String>,
            scope: SearchScope
    ) {
        val oldName = field.getName()
        val processor = ServiceManager.getService(field.getProject(), javaClass<AndroidUIXmlProcessor>())
        renameSyntheticProperties(allRenames, newName, oldName, processor)
    }

}
