/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill

import shadow.org.jdom2.Document
import shadow.org.jdom2.Element
import shadow.org.jdom2.Namespace
import shadow.org.jdom2.output.Format
import shadow.org.jdom2.output.XMLOutputter

fun xml(name: String, vararg args: Pair<String, Any>, block: XmlNode.() -> Unit = {}): XmlNode {
    return XmlNode(name, args.asList(), block)
}

class XmlNode(val name: String, private val args: List<Pair<String, Any>>, block: XmlNode.() -> Unit = {}) {
    private val children = mutableListOf<XmlNode>()
    private var value: Any? = null

    private val namespaces = ArrayList<Namespace>(0)

    init {
        @Suppress("UNUSED_EXPRESSION")
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

        for ((rawKey, rawValue) in args) {
            val value = rawValue.toString()

            when {
                rawKey == "xmlns" -> namespaces += Namespace.getNamespace(value)
                rawKey.startsWith("xmlns:") -> namespaces += Namespace.getNamespace(rawKey.drop("xmlns:".length), value)
                else -> {
                    val namespace = rawKey.substringBefore(':', missingDelimiterValue = "")
                    val key = rawKey.substringAfter(':', missingDelimiterValue = rawKey)

                    if (namespace.isNotEmpty()) {
                        element.setAttribute(key, value, Namespace.getNamespace(namespace, "https://foo"))
                    } else {
                        element.setAttribute(key, value)
                    }
                }
            }
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
        val document = Document()

        val rootElement = toElement()
        document.rootElement = rootElement

        for (namespace in namespaces) {
            rootElement.addNamespaceDeclaration(namespace)
        }

        val output = XMLOutputter().also { it.format = Format.getPrettyFormat() }
        return output.outputString(document)
    }
}