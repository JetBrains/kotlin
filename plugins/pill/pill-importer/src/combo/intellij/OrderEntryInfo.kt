/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.combo.intellij

import org.jetbrains.kotlin.pill.PathContext
import org.jetbrains.kotlin.pill.getUrlWithVariables
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File

enum class OrderEntrySetKind {
    CLASSES, JAVADOC, SOURCES
}

@Suppress("unused")
enum class OrderEntryScope {
    COMPILE, PROVIDED, RUNTIME, TEST;

    companion object {
        fun get(orderEntry: Element): OrderEntryScope {
            val rawScope = orderEntry.getAttribute("scope")
            return values().firstOrNull { it.name == rawScope } ?: COMPILE
        }
    }
}

data class ScopedOrderEntryInfo(val entry: OrderEntryInfo, val scope: OrderEntryScope) {
    fun render(document: Document, pathContext: PathContext): Element {
        return entry.render(document, scope, pathContext)
    }
}

data class LibraryInfo(
    val classes: List<File>,
    val javadoc: List<File> = emptyList(),
    val sources: List<File> = emptyList()
) {
    companion object {
        fun parse(library: Element, pathContext: PathContext): LibraryInfo {
            val classes = mutableListOf<File>()
            val javadoc = mutableListOf<File>()
            val sources = mutableListOf<File>()

            for (entrySet in library.childElements) {
                val consumer = when (entrySet.tagName) {
                    OrderEntrySetKind.CLASSES.name -> classes
                    OrderEntrySetKind.JAVADOC.name -> javadoc
                    OrderEntrySetKind.SOURCES.name -> sources
                    else -> error("Unknown entry set kind ${entrySet.tagName}")
                }

                for (root in entrySet.getElementsByTagName("root").elements) {
                    val url = root.getAttribute("url") ?: continue
                    val path = pathContext.substituteWithValues(getUrlPath(url))
                    consumer += File(path).canonicalFile
                }
            }

            return LibraryInfo(classes, javadoc, sources)
        }
    }
}

sealed class OrderEntryInfo {
    companion object {
        fun parse(orderEntry: Element, pathContext: PathContext): ScopedOrderEntryInfo {
            val entry = when (val type = orderEntry.getAttribute("type")) {
                "inheritedJdk" -> InheritedJdk
                "sourceFolder" -> {
                    val forTests = orderEntry.getAttribute("forTests") == "true"
                    SourceFolder(forTests)
                }
                "library" -> {
                    val name = orderEntry.getAttribute("name") ?: error("'name' attribute was not found")
                    val level = orderEntry.getAttribute("level") ?: error("'level' attribute was not found")
                    Library(name, level)
                }
                "module" -> {
                    val moduleName = orderEntry.getAttribute("module-name") ?: error("'module-name' attribute was not found")
                    ModuleOutput(moduleName)
                }
                "module-library" -> {
                    val libraryElement = orderEntry.getElementsByTagName("library").elements.single()
                    val library = LibraryInfo.parse(libraryElement, pathContext)
                    ModuleLibrary(library)
                }
                else -> error("Unknown entry type $type")
            }

            val scope = OrderEntryScope.get(orderEntry)
            return ScopedOrderEntryInfo(entry, scope)
        }
    }

    object InheritedJdk : OrderEntryInfo() {
        override fun render(document: Document, scope: OrderEntryScope, pathContext: PathContext): Element {
            return document.createElement("orderEntry").apply {
                setAttribute("type", "inheritedJdk")
            }
        }
    }

    class SourceFolder(val forTests: Boolean) : OrderEntryInfo() {
        override fun render(document: Document, scope: OrderEntryScope, pathContext: PathContext): Element {
            return document.createElement("orderEntry").apply {
                setAttribute("type", "sourceFolder")
                setAttribute("forTests", forTests.toString())
            }
        }
    }

    data class Library(val name: String, val level: String = "project") : OrderEntryInfo() {
        override fun render(document: Document, scope: OrderEntryScope, pathContext: PathContext): Element {
            return document.createElement("orderEntry").apply {
                setAttribute("type", "library")
                setAttribute("scope", scope.name)
                setAttribute("name", name)
                setAttribute("level", "project")
            }
        }
    }

    data class ModuleOutput(val moduleName: String) : OrderEntryInfo() {
        override fun render(document: Document, scope: OrderEntryScope, pathContext: PathContext): Element {
            return document.createElement("orderEntry").apply {
                setAttribute("type", "module")
                setAttribute("scope", scope.name)
                setAttribute("module-name", moduleName)
            }
        }
    }

    data class ModuleLibrary(val library: LibraryInfo) : OrderEntryInfo() {
        override fun render(document: Document, scope: OrderEntryScope, pathContext: PathContext): Element {
            return document.createElement("orderEntry").apply {
                setAttribute("type", "module-library")
                setAttribute("scope", scope.name)

                val libraryElement = document.createElement("library").apply {
                    fun createEntrySet(rootSetName: String, files: List<File>) {
                        val entrySet = document.createElement(rootSetName).apply {
                            for (file in files) {
                                val url = pathContext.getUrlWithVariables(file)
                                val rootElement = document.createElement("root").apply { setAttribute("url", url) }
                                appendChild(rootElement)
                            }
                        }

                        appendChild(entrySet)
                    }

                    createEntrySet(OrderEntrySetKind.CLASSES.name, library.classes)
                    createEntrySet(OrderEntrySetKind.JAVADOC.name, library.javadoc)
                    createEntrySet(OrderEntrySetKind.SOURCES.name, library.sources)
                }

                appendChild(libraryElement)
            }
        }
    }

    abstract fun render(document: Document, scope: OrderEntryScope, pathContext: PathContext): Element
}