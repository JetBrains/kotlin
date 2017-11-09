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
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.androidIdToName
import org.jetbrains.kotlin.android.synthetic.isWidgetTypeIgnored
import org.jetbrains.kotlin.android.synthetic.res.ResourceIdentifier

class AndroidXmlVisitor(val elementCallback: (ResourceIdentifier, String, XmlAttribute) -> Unit) : XmlElementVisitor() {

    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }

    override fun visitXmlElement(element: XmlElement?) {
        element?.acceptChildren(this)
    }

    override fun visitXmlTag(tag: XmlTag?) {
        val localName = tag?.localName ?: ""
        if (isWidgetTypeIgnored(localName)) {
            tag?.acceptChildren(this)
            return
        }

        val idAttribute = tag?.getAttribute(AndroidConst.ID_ATTRIBUTE_NO_NAMESPACE, AndroidConst.ANDROID_NAMESPACE)
        if (idAttribute != null) {
            val idAttributeValue = idAttribute.value
            if (idAttributeValue != null) {
                val xmlType = tag.getAttribute(AndroidConst.CLASS_ATTRIBUTE_NO_NAMESPACE)?.value ?: localName
                val name = androidIdToName(idAttributeValue)
                if (name != null) elementCallback(name, xmlType, idAttribute)
            }
        }

        tag?.acceptChildren(this)
    }
}