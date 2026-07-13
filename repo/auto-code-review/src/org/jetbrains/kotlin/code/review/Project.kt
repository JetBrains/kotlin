/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@JvmInline
@Serializable
value class ProjectFilePath(val pathFromProjectRoot: String) {
    init {
        check(
            pathFromProjectRoot.isNotEmpty() &&
                    pathFromProjectRoot.split('/').none { it.isEmpty() || it == "." || it == ".." }
        ) { """Invalid path: "$pathFromProjectRoot"""" }
    }

    override fun toString(): String = pathFromProjectRoot

    val fileName: String
        get() = pathFromProjectRoot.substringAfterLast('/')

    val dir: ProjectDirPath
        get() = ProjectDirPath(pathFromProjectRoot.substringBeforeLast('/', missingDelimiterValue = ""))
}

@JvmInline
value class ProjectDirPath(val pathFromProjectRoot: String) {
    init {
        check(
            pathFromProjectRoot.isEmpty() ||
                    pathFromProjectRoot.split('/').none { it.isEmpty() || it == "." || it == ".." }
        ) { """Invalid path: "$pathFromProjectRoot"""" }
    }

    override fun toString(): String = if (pathFromProjectRoot.isEmpty()) {
        "./"
    } else {
        "$pathFromProjectRoot/"
    }

    val parent: ProjectDirPath?
        get() = if (pathFromProjectRoot == "") {
            null
        } else {
            ProjectDirPath(pathFromProjectRoot.substringBeforeLast('/', missingDelimiterValue = ""))
        }

    private val filePathPrefix: String
        get() = if (pathFromProjectRoot == "") {
            ""
        } else {
            "$pathFromProjectRoot/"
        }

    fun file(relativePath: String): ProjectFilePath {
        if (relativePath.startsWith("../")) {
            val parent = this.parent ?: error("Cannot go up from root, but need to resolve $relativePath")
            return parent.file(relativePath.removePrefix("../"))
        }
        return ProjectFilePath(pathFromProjectRoot = "$filePathPrefix$relativePath")
    }

    /**
     * If [filePath] is inside this directory, returns the relative path from this to [filePath].
     * In such a case, this is basically the opposite of [file].
     *
     * Otherwise, returns null.
     */
    fun relativePathToFileInside(filePath: ProjectFilePath): String? {
        val prefix = this.filePathPrefix
        return if (filePath.pathFromProjectRoot.startsWith(prefix)) {
            filePath.pathFromProjectRoot.removePrefix(prefix)
        } else {
            null
        }
    }
}

interface Project {
    suspend fun readLines(path: ProjectFilePath): List<String>?
    suspend fun fileExists(path: ProjectFilePath): Boolean
}

class LocalProject(val root: File) : Project {
    private fun ProjectFilePath.toFile(): File = root.resolve(pathFromProjectRoot)

    override suspend fun readLines(path: ProjectFilePath): List<String>? = withContext(Dispatchers.IO) {
        path.toFile().takeIf { it.exists() }?.readLines()
    }

    override suspend fun fileExists(path: ProjectFilePath): Boolean = withContext(Dispatchers.IO) {
        path.toFile().exists()
    }
}
