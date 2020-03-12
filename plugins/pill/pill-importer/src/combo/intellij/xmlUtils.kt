/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.combo.intellij

import org.w3c.dom.*
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun File.loadXml(): Document {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    return builder.parse(this)
}

fun Document.saveXml(file: File, prettyPrint: Boolean = true) {
    file.parentFile.mkdirs()
    file.writer().use {
        val tr = TransformerFactory.newInstance().newTransformer()
        if (prettyPrint) {
            tr.setOutputProperty(OutputKeys.INDENT, "yes")
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }
        tr.transform(DOMSource(this), StreamResult(it))
    }
}

val Node.childElements: List<Element>
    get() = childNodes.elements

val NodeList.elements: List<Element>
    get() = asList().filterIsInstance<Element>()


fun NodeList.asList(): List<Node> {
    return object : AbstractList<Node>(), RandomAccess {
        override val size: Int get() = this@asList.length
        override fun isEmpty(): Boolean = this@asList.length <= 0
        override fun get(index: Int): Node = this@asList.item(index)
    }
}