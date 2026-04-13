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
 * Use [relativePomPath] to target a submodule's pom (e.g. "app/pom.xml").
 */
fun MavenTestProject.modifyPomXml(relativePomPath: String = "pom.xml", block: Document.() -> Unit) {
    val pomFile = workDir.resolve(relativePomPath).toFile()
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
fun MavenTestProject.addPluginLevelConfiguration(xmlFragment: String, relativePomPath: String = "pom.xml") {
    modifyPomXml(relativePomPath) {
        val plugin = findKotlinPlugin()
        val config = plugin.getOrCreateChild("configuration")
        config.appendXmlFragment(xmlFragment)
    }
}

/**
 * Configures `<jdkToolchain>` on kotlin-maven-plugin to select a specific JDK version from toolchains.xml.
 */
fun MavenTestProject.configureJdkToolchain(version: String, relativePomPath: String = "pom.xml") {
    addPluginLevelConfiguration("<jdkToolchain><version>$version</version></jdkToolchain>", relativePomPath)
}

/**
 * Changes the JDK version in an existing `maven-toolchains-plugin` configuration.
 */
fun MavenTestProject.modifyMavenToolchainsPluginJdkVersion(newVersion: String, relativePomPath: String = "pom.xml") {
    modifyPomXml(relativePomPath) {
        val plugins = getElementsByTagName("plugin")
        val toolchainsPlugin = (0 until plugins.length)
            .map { plugins.item(it) as Element }
            .firstOrNull { it.getElementsByTagName("artifactId").item(0)?.textContent == "maven-toolchains-plugin" }
            ?: error("maven-toolchains-plugin not found in pom.xml")
        val jdkVersionNodes = toolchainsPlugin.getElementsByTagName("version")
        val jdkVersion = (0 until jdkVersionNodes.length)
            .map { jdkVersionNodes.item(it) }
            .first { it.parentNode.nodeName == "jdk" }
        jdkVersion.textContent = newVersion
    }
}

/**
 * Adds `maven-toolchains-plugin` to select a JDK toolchain.
 * When [profileId] is null, adds to `<build><plugins>` directly.
 * When [profileId] is set, wraps in a `<profile>` activated via `-P`.
 */
fun MavenTestProject.addMavenToolchainsPlugin(version: String, profileId: String? = null, relativePomPath: String = "pom.xml") {
    val pluginXml = """
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-toolchains-plugin</artifactId>
            <version>3.2.0</version>
            <executions>
                <execution>
                    <goals><goal>toolchain</goal></goals>
                </execution>
            </executions>
            <configuration>
                <toolchains>
                    <jdk>
                        <version>$version</version>
                    </jdk>
                </toolchains>
            </configuration>
        </plugin>
    """.trimIndent()

    modifyPomXml(relativePomPath) {
        if (profileId != null) {
            val profiles = documentElement.getOrCreateChild("profiles")
            profiles.appendXmlFragment("""
                <profile>
                    <id>$profileId</id>
                    <build><plugins>$pluginXml</plugins></build>
                </profile>
            """.trimIndent())
        } else {
            val build = documentElement.getOrCreateChild("build")
            val plugins = build.getOrCreateChild("plugins")
            plugins.appendXmlFragment(pluginXml)
        }
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
