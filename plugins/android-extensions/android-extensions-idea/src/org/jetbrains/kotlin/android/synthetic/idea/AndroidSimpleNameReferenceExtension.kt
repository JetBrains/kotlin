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

import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.util.AndroidResourceUtil
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.androidIdToName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.plugin.references.SimpleNameReferenceExtension
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

class AndroidSimpleNameReferenceExtension : SimpleNameReferenceExtension {

    override fun isReferenceTo(reference: KtSimpleNameReference, element: PsiElement): Boolean =
            element is XmlFile && reference.isReferenceToXmlFile(element)

    private fun isLayoutPackageIdentifier(reference: KtSimpleNameReference): Boolean {
        val probablyVariant = reference.element?.parent as? KtDotQualifiedExpression ?: return false
        val probablyKAS = probablyVariant.receiverExpression as? KtDotQualifiedExpression ?: return false
        return probablyKAS.receiverExpression.text == AndroidConst.SYNTHETIC_PACKAGE
    }

    override fun handleElementRename(reference: KtSimpleNameReference, psiFactory: KtPsiFactory, newElementName: String): PsiElement? {
        val resolvedElement = reference.resolve()
        if (resolvedElement is XmlAttributeValue && AndroidResourceUtil.isIdDeclaration(resolvedElement)) {
            val newSyntheticPropertyName = androidIdToName(newElementName) ?: return null
            return psiFactory.createNameIdentifier(newSyntheticPropertyName.name)
        }
        else if (isLayoutPackageIdentifier(reference)) {
            return psiFactory.createSimpleName(newElementName.removeSuffix(".xml")).getIdentifier()
        }

        return null
    }

    private fun KtSimpleNameReference.isReferenceToXmlFile(xmlFile: XmlFile): Boolean {
        if (!isLayoutPackageIdentifier(this)) {
            return false
        }

        if (xmlFile.name.removeSuffix(".xml") != element.getReferencedName()) {
            return false
        }

        val virtualFile = xmlFile.virtualFile ?: return false
        val layoutDir = virtualFile.parent
        if (layoutDir.name != "layout" && !layoutDir.name.startsWith("layout-")) {
            return false
        }

        val resourceDirectories = AndroidFacet.getInstance(element)?.resourceFolderManager?.folders ?: return false
        val resourceDirectory = virtualFile.parent?.parent ?: return false
        return resourceDirectories.any { it == resourceDirectory }
    }
}