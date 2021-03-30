/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.artifact

import org.jetbrains.kotlin.pill.util.PathContext
import org.jetbrains.kotlin.pill.util.XmlNode
import org.jetbrains.kotlin.pill.util.xml
import java.io.File

sealed class ArtifactElement {
    private val myChildren = mutableListOf<ArtifactElement>()
    private val children get() = myChildren

    fun add(child: ArtifactElement) {
        myChildren += child
    }

    fun add(children: List<ArtifactElement>) {
        myChildren += children
    }

    abstract fun render(context: PathContext): XmlNode

    fun renderRecursively(context: PathContext): XmlNode {
        return render(context).apply {
            children.forEach { add(it.renderRecursively(context)) }
        }
    }

    class Root : ArtifactElement() {
        override fun render(context: PathContext) = xml("root", "id" to "root")
    }

    data class Directory(val name: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "directory", "name" to name)
    }

    data class Archive(val name: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "archive", "name" to name)
    }

    data class ModuleOutput(val moduleName: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "module-output", "name" to moduleName)
    }

    data class FileCopy(val source: File, val outputFileName: String? = null) : ArtifactElement() {
        override fun render(context: PathContext): XmlNode {
            val args = mutableListOf("id" to "file-copy", "path" to context(source))
            if (outputFileName != null) {
                args += "output-file-name" to outputFileName
            }

            return xml("element", *args.toTypedArray())
        }
    }

    data class DirectoryCopy(val source: File) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "dir-copy", "path" to context(source))
    }

    data class ProjectLibrary(val name: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "library", "level" to "project", "name" to name)
    }

    data class ExtractedDirectory(val archive: File, val pathInJar: String = "/") : ArtifactElement() {
        override fun render(context: PathContext) =
            xml("element", "id" to "extracted-dir", "path" to context(archive), "path-in-jar" to pathInJar)
    }
}