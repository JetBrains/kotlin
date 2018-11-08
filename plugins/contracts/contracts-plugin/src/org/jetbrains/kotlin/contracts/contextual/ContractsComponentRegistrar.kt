/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual


import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.io.URLUtil
import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.jdom.input.sax.SAXHandler
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.contracts.contextual.extensions.SpecificContractExtension
import org.jetbrains.kotlin.contracts.contextual.serialization.ExtensionContractSerializer
import org.jetbrains.kotlin.extensions.ContractsExtension
import org.jetbrains.kotlin.serialization.ContractSerializerExtension
import org.xml.sax.XMLReader
import java.io.File
import java.io.InputStream
import java.net.URLClassLoader
import javax.xml.XMLConstants

class ContractsComponentRegistrar : ComponentRegistrar {
    private class ContractsPluginDescriptor(private val classLoader: ClassLoader) : PluginDescriptor {
        override fun getPluginClassLoader(): ClassLoader = classLoader

        override fun getPluginId(): PluginId = PluginId.getId(SpecificContractExtension.NAME)
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        ContractsExtension.registerExtension(project, ContractsImplementationExtension())
        ContractSerializerExtension.registerExtension(project, ExtensionContractSerializer())

        val jarPaths = configuration.getList(ContractsConfigurationKeys.PATHS_TO_JARS)
        val classLoader = URLClassLoader(
            jarPaths.map { File(it).toURI().toURL() }.toTypedArray(),
            this::class.java.classLoader
        )

        val pluginDescriptor = ContractsPluginDescriptor(classLoader)

        val extensionsArea = Extensions.getArea(project)
        if (!extensionsArea.hasExtensionPoint(SpecificContractExtension.extensionPointName)) {
            val pluginXmlUrl = this::class.java.classLoader.getResourceAsStream("META-INF/plugin.xml")
            val pluginElement = loadElement(pluginXmlUrl)
            val extensionPointElement = pluginElement.getChild("extensionPoints").getChild("extensionPoint")
            extensionsArea.registerExtensionPoint(pluginDescriptor, extensionPointElement)
        }

        val extensionPoint = extensionsArea.getExtensionPoint(SpecificContractExtension.extensionPointName)

        val subpluginXmlUrls = classLoader.findResources("META-INF/plugin.xml").toList()

        for (url in subpluginXmlUrls) {
            val pluginElement = loadElement(URLUtil.openStream(url))
            for (element in pluginElement.getChild("extensions").getChildren("specificContractExtension")) {
                extensionsArea.registerExtension(extensionPoint, pluginDescriptor, element)
            }
        }
    }

    companion object {
        private fun loadElement(stream: InputStream): Element = stream.reader().use { saxBuilder.build(it) }.detachRootElement()

        private val saxBuilder: SAXBuilder = object : SAXBuilder() {
            override fun configureParser(parser: XMLReader, contentHandler: SAXHandler?) {
                super.configureParser(parser, contentHandler)
                try {
                    parser.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                } catch (ignore: Exception) {
                }
            }
        }.apply {
            ignoringBoundaryWhitespace = true
            ignoringElementContentWhitespace = true
        }
    }

}
