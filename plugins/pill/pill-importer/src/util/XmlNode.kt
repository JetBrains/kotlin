/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.util

import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter

fun xml(name: String, vararg args: Pair<String, Any>, block: XmlNode.() -> Unit = {}): XmlNode {
    return XmlNode(name, args.asList(), block)
}

class XmlNode(val name: String, private val args: List<Pair<String, Any>>, block: XmlNode.() -> Unit = {}) {
    private val children = mutableListOf<XmlNode>()
    private var value: Any? = null

    init {
        block()
    }

    fun xml(name: String, vararg args: Pair<String, Any>, block: XmlNode.() -> Unit = {}) {
        children += XmlNode(name, args.asList(), block = block)
    }

    fun add(xml: XmlNode) {
        children += xml
    }

    fun raw(text: String) {
        value = text
    }

    private fun toElement(): Element {
        val element = Element(name)

        for (arg in args) {
            element.setAttribute(arg.first, arg.second.toString())
        }

        require(value == null || children.isEmpty())

        value?.let { value ->
            element.addContent(value.toString())
        }

        for (child in children) {
            element.addContent(child.toElement())
        }

        return element
    }

    override fun toString(): String {
        val document = Document().also { it.rootElement = toElement() }
        val output = XMLOutputter().also { it.format = Format.getPrettyFormat() }
        return output.outputString(document)
    }
}