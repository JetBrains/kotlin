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

package org.jetbrains.kotlin.android.synthetic

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.HashMap

class AndroidXmlHandler(private val elementCallback: (String, String) -> Unit) : DefaultHandler() {

    override fun startDocument() {
        super.startDocument()
    }

    override fun endDocument() {
        super.endDocument()
    }

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        if (isWidgetTypeIgnored(localName)) return
        val attributesMap = attributes.toMap()
        val idAttribute = attributesMap[AndroidConst.ID_ATTRIBUTE_NO_NAMESPACE]
        val widgetType = attributesMap[AndroidConst.CLASS_ATTRIBUTE_NO_NAMESPACE] ?: localName
        val name = idAttribute?.let { androidIdToName(idAttribute) }
        if (name != null) elementCallback(name, widgetType)
    }

    override fun endElement(uri: String?, localName: String, qName: String) {

    }

}

public fun Attributes.toMap(): HashMap<String, String> {
    val res = HashMap<String, String>()
    for (index in 0..length - 1) {
        val attrName = getLocalName(index)!!
        val attrVal = getValue(index)!!
        res[attrName] = attrVal
    }
    return res
}


