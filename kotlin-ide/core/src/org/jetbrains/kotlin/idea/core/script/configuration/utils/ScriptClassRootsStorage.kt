/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element
import org.jetbrains.kotlin.idea.core.script.debug

@State(
    name = "ScriptClassRootsStorage",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class ScriptClassRootsStorage : PersistentStateComponent<Element> {

    private var classpath: MutableMap<String, Set<String>> = hashMapOf()
    private var sources: MutableMap<String, Set<String>> = hashMapOf()
    private var sdks: MutableMap<String, Set<String>> = hashMapOf()

    override fun getState(): Element {
        val root = Element("ScriptClassRootsStorage")

        storeCollection(root, classpath, "classpath")
        storeCollection(root, sources, "sources")
        storeCollection(root, sdks, "sdk")

        return root
    }

    private fun storeCollection(root: Element, col: Map<String, Set<String>>, name: String) {
        for ((key, paths) in col) {
            for (path in paths) {
                val element = Element(name)
                element.setAttribute("path", path)
                element.setAttribute("key", key)
                root.addContent(element)
            }
        }
    }

    override fun loadState(state: Element) {
        classpath = readCollection(state, "classpath")
        sources = readCollection(state, "sources")
        sdks = readCollection(state, "sdks")
    }

    private fun readCollection(root: Element, name: String): MutableMap<String, Set<String>> {
        val result: MutableMap<String, HashSet<String>> = hashMapOf()
        for (it in root.getChildren(name)) {
            result.getOrPut(it.getAttributeValue("key")) {
                hashSetOf()
            }.add(it.getAttributeValue("path"))
        }

        @Suppress("UNCHECKED_CAST")
        return result as MutableMap<String, Set<String>>
    }

    private fun toStringNames(sdks: Collection<Sdk>): Set<String> {
        return sdks.map { it.name }.toSet()
    }

    private fun toVirtualFiles(prop: Set<String>?, sources: Boolean): List<VirtualFile> {
        if (prop == null) return emptyList()

        val rootType = if (sources) OrderRootType.SOURCES else OrderRootType.CLASSES
        return prop.mapNotNull { ProjectJdkTable.getInstance().findJdk(it) }
            .flatMap { it.rootProvider.getFiles(rootType).toList() }
    }

    private fun toVirtualFiles(prop: Collection<String>?): List<VirtualFile> {
        if (prop == null) return emptyList()

        return prop.mapNotNull {
            if (it.endsWith(JarFileSystem.PROTOCOL)) {
                StandardFileSystems.jar()?.findFileByPath(it + JarFileSystem.JAR_SEPARATOR)?.let {
                    return@mapNotNull it
                }
            }

            StandardFileSystems.local()?.findFileByPath(it)?.let {
                return@mapNotNull it
            }

            // TODO: report this somewhere, but do not throw: assert(res != null, { "Invalid classpath entry '$this': exists: ${exists()}, is directory: $isDirectory, is file: $isFile" })

            null
        }.distinct()
    }

    fun containsAll(key: Key, configuration: ScriptClassRoots): Boolean {
        if (configuration.classpathFiles.isNotEmpty() && classpath[key.value]?.containsAll(configuration.classpathFiles) != true) {
            debug { "class roots were changed: old = $classpath, new = ${configuration.classpathFiles}" }
            return false
        }
        if (configuration.sourcesFiles.isNotEmpty() && sources[key.value]?.containsAll(configuration.sourcesFiles) != true) {
            debug { "source roots were changed: old = $sources, new = ${configuration.sourcesFiles}" }
            return false
        }
        if (configuration.sdks.isNotEmpty() && sdks[key.value]?.containsAll(toStringNames(configuration.sdks)) != true) {
            debug { "sdk classes were changed: old = $sdks, new = ${configuration.sdks.map { it.homePath }}" }
            return false
        }
        return true
    }

    fun save(key: Key, configuration: ScriptClassRoots) {
        // TODO: do not drop all storage on save: KT-34444
        classpath.getOrPut(key.value, { emptySet() })
        classpath.replace(key.value, configuration.classpathFiles)

        sources.getOrPut(key.value, { emptySet() })
        sources.replace(key.value, configuration.sourcesFiles)

        sdks.getOrPut(key.value, { emptySet() })
        sdks.replace(key.value, toStringNames(configuration.sdks))
    }

    fun loadClasspathRoots(key: Key): List<VirtualFile> {
        return toVirtualFiles(sdks[key.value], false) + toVirtualFiles(classpath[key.value])
    }

    fun loadSourcesRoots(key: Key): List<VirtualFile> {
        return toVirtualFiles(sdks[key.value], true) + toVirtualFiles(sources[key.value])
    }

    companion object {
        fun getInstance(project: Project): ScriptClassRootsStorage =
            ServiceManager.getService(project, ScriptClassRootsStorage::class.java)

        data class ScriptClassRoots(
            val classpathFiles: Set<String>,
            val sourcesFiles: Set<String>,
            val sdks: Set<Sdk>
        )

        val EMPTY = ScriptClassRoots(emptySet(), emptySet(), emptySet())

        data class Key(val value: String)
    }
}