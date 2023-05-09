/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.library.uniqueName
import java.io.File

internal data class ObjCExportConfig(
    val frameworks: List<Framework>,
) {
    data class Framework(val name: String, val topLevelPrefix: String, val libraries: List<Library>)
    data class Library(val name: String, val exported: Boolean)
}

private fun parseLibrary(element: Element): ObjCExportConfig.Library {
    val name = element.getChildText("name")
    val exported = element.getChildText("exported") == "true"
    return ObjCExportConfig.Library(name, exported)
}

internal class ObjCExportConfigParser(
        private val config: KonanConfig,
        private val allModules: List<ModuleDescriptor>,
) {
    fun readObjCExportConfigFromXml(file: File): List<ObjCExportFrameworkStructure> {
        val result = mutableListOf<ObjCExportFrameworkStructure>()
        val root = file.readXml()
        result += root.children.map { frameworkNode ->
            val frameworkName = frameworkNode.getChild("name").value
            val topLevelPrefix = frameworkNode.getChild("topLevelPrefix").value
            val libraries = frameworkNode.getChild("libraries").children.map { libraryNode ->
                parseLibrary(libraryNode)
            }
            val headerStrategy: ObjCExportHeaderStrategy = parseHeaderStrategy(frameworkName, frameworkNode.getChild("headerStrategy"))
            ObjCExportFrameworkStructure(
                    topLevelPrefix = topLevelPrefix,
                    name = frameworkName,
                    modulesInfo = libraries.map { it.resolve() },
                    headerStrategy = headerStrategy,
            )
        }
        return result
    }

    private fun parseHeaderStrategy(frameworkName: String, element: Element): ObjCExportHeaderStrategy {
        val kind = element.getChildText("kind")
        return when (kind) {
            "global" -> {
                ObjCExportHeaderStrategy.Global(frameworkName, element.getChildText("headerName"))
            }
            else -> error("Unsupported header strategy: $kind")
        }
    }

    private fun ObjCExportConfig.Library.resolve(): ObjCExportModuleInfo {
        val resolvedLibrary = config.resolvedLibraries.getFullList(TopologicalLibraryOrder)
                .find { it.uniqueName == name }
                ?: error("Library $name not found. Is it passed to the compiler?")
        val module = allModules.filter { it.klibModuleOrigin is DeserializedKlibModuleOrigin }.first { it.kotlinLibrary == resolvedLibrary }
        return ObjCExportModuleInfo(module = module, exported = exported)
    }

    private fun File.readXml(): Element {
        return inputStream().use { SAXBuilder().build(it).rootElement }
    }
}