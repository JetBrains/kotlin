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

import org.jetbrains.kotlin.plugin.references.SimpleNameReferenceExtension
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import com.intellij.psi.PsiElement
import org.jetbrains.android.dom.wrappers.ValueResourceElementWrapper
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.lang.resolve.android.isAndroidSyntheticElement

public class AndroidSimpleNameReferenceExtension : SimpleNameReferenceExtension {
    override fun isReferenceTo(reference: JetSimpleNameReference, element: PsiElement): Boolean? {
        val resolvedElement = reference.resolve() ?: return null

        if (isAndroidSyntheticElement(resolvedElement)) {
            if (element is ValueResourceElementWrapper) {
                val resource = element.getValue()
                return (resolvedElement as JetProperty).getName() == resource.substring(resource.indexOf('/') + 1)
            }
        }
        return null
    }

    override fun handleElementRename(reference: JetSimpleNameReference, psiFactory: JetPsiFactory, newElementName: String): PsiElement? {
        return if (newElementName.startsWith("@+id/"))
            psiFactory.createNameIdentifier(newElementName.substring(newElementName.indexOf('/') + 1))
        else
            null
    }
}