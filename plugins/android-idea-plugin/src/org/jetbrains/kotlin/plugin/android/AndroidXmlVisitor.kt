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

import org.jetbrains.kotlin.lang.resolve.android.AndroidResourceManager
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.lang.resolve.android.AndroidConst
import org.jetbrains.kotlin.lang.resolve.android
import org.jetbrains.kotlin.lang.resolve.android.idToName

class AndroidXmlVisitor(
        val resourceManager: AndroidResourceManager,
        val elementCallback: (String, String, XmlAttribute) -> Unit
) : XmlElementVisitor() {

    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }

    override fun visitXmlElement(element: XmlElement?) {
        element?.acceptChildren(this)
    }

    override fun visitXmlTag(tag: XmlTag?) {
        val attribute = tag?.getAttribute(AndroidConst.ID_ATTRIBUTE)
        if (attribute != null) {
            val attributeValue = attribute.getValue()
            if (attributeValue != null) {
                val classNameAttr = tag?.getAttribute(AndroidConst.CLASS_ATTRIBUTE_NO_NAMESPACE)?.getValue() ?: tag?.getLocalName()
                val name = idToName(attributeValue)
                if (name != null) elementCallback(name, classNameAttr!!, attribute)
            }
        }
        tag?.acceptChildren(this)
    }
}