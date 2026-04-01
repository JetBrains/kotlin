/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Parses pom.xml, applies modifications via [block], and writes it back.
 */
fun MavenTestProject.modifyPomXml(block: Document.() -> Unit) {
    val pomFile = workDir.resolve("pom.xml").toFile()
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomFile)
    doc.block()
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
    transformer.transform(DOMSource(doc), StreamResult(pomFile))
}

/**
 * Finds the kotlin-maven-plugin `<plugin>` element in the pom.xml.
 */
fun Document.findKotlinPlugin(): Element {
    val plugins = getElementsByTagName("plugin")
    for (i in 0 until plugins.length) {
        val plugin = plugins.item(i) as Element
        val artifactId = plugin.getElementsByTagName("artifactId").item(0)?.textContent
        if (artifactId == "kotlin-maven-plugin") return plugin
    }
    error("kotlin-maven-plugin not found in pom.xml")
}

/**
 * Gets or creates a direct child element with the given [tagName].
 */
fun Element.getOrCreateChild(tagName: String): Element {
    val children = childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        if (child is Element && child.tagName == tagName) return child
    }
    val newElement = ownerDocument.createElement(tagName)
    appendChild(newElement)
    return newElement
}

/**
 * Finds an `<execution>` element by its `<id>`.
 */
fun Element.findExecution(executionId: String): Element {
    val executions = getElementsByTagName("execution")
    for (i in 0 until executions.length) {
        val execution = executions.item(i) as Element
        val id = execution.getElementsByTagName("id").item(0)?.textContent
        if (id == executionId) return execution
    }
    error("Execution with id '$executionId' not found")
}

/**
 * Appends an XML fragment (as a string) into this element.
 */
fun Element.appendXmlFragment(xmlFragment: String) {
    val wrappedXml = "<root>$xmlFragment</root>"
    val fragmentDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(ByteArrayInputStream(wrappedXml.toByteArray()))
    val children = fragmentDoc.documentElement.childNodes
    for (i in 0 until children.length) {
        val imported = ownerDocument.importNode(children.item(i), true)
        appendChild(imported)
    }
}

/**
 * Adds XML content to the plugin-level `<configuration>` of kotlin-maven-plugin.
 */
fun MavenTestProject.addPluginLevelConfiguration(xmlFragment: String) {
    modifyPomXml {
        val plugin = findKotlinPlugin()
        val config = plugin.getOrCreateChild("configuration")
        config.appendXmlFragment(xmlFragment)
    }
}

/**
 * Adds XML content to the compile execution's `<configuration>`.
 */
fun MavenTestProject.addToCompileExecutionConfiguration(xmlFragment: String) {
    modifyPomXml {
        val plugin = findKotlinPlugin()
        val execution = plugin.findExecution("compile")
        val config = execution.getOrCreateChild("configuration")
        config.appendXmlFragment(xmlFragment)
    }
}

/**
 * Adds XML content to the test-compile execution's `<configuration>`.
 */
fun MavenTestProject.addToTestCompileExecutionConfiguration(xmlFragment: String) {
    modifyPomXml {
        val plugin = findKotlinPlugin()
        val execution = plugin.findExecution("test-compile")
        val config = execution.getOrCreateChild("configuration")
        config.appendXmlFragment(xmlFragment)
    }
}

/**
 * Adds `<pluginOptions>` with given options to the kotlin-maven-plugin configuration.
 */
fun MavenTestProject.addPluginOptions(vararg options: String) {
    modifyPomXml {
        val plugin = findKotlinPlugin()
        val config = plugin.getOrCreateChild("configuration")
        val pluginOptions = config.getOrCreateChild("pluginOptions")
        for (option in options) {
            pluginOptions.appendXmlFragment("<option>$option</option>")
        }
    }
}

/**
 * Adds a Maven profile with given properties to the pom.xml.
 */
fun MavenTestProject.addMavenProfile(profileId: String, vararg properties: Pair<String, String>) {
    modifyPomXml {
        val profiles = documentElement.getOrCreateChild("profiles")
        val profile = createElement("profile")
        profiles.appendChild(profile)
        profile.appendXmlFragment("<id>$profileId</id>")
        val props = createElement("properties")
        profile.appendChild(props)
        for ((key, value) in properties) {
            props.appendXmlFragment("<$key>$value</$key>")
        }
    }
}
