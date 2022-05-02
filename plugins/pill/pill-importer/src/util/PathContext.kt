/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.util

import org.gradle.api.Project
import org.jetbrains.kotlin.pill.model.PModule
import org.jetbrains.kotlin.pill.model.PProject
import java.io.File
import java.util.*

private val USER_HOME_DIR_PATH = System.getProperty("user.home").withSlash()

private fun replacePrefix(path: String, prefix: String, variableName: String): String {
    if (path.startsWith(prefix)) {
        return "$" + variableName + "$/" + path.drop(prefix.length)
    }

    return path
}

private fun simplifyUserHomeDirPath(path: String): String {
    return replacePrefix(path, USER_HOME_DIR_PATH, "USER_HOME")
}

interface PathContext {
    operator fun invoke(file: File): String

    fun url(file: File): Pair<String, String> {
        val path = when {
            file.isFile && file.extension.lowercase(Locale.getDefault()) == "jar" -> "jar://" + this(file) + "!/"
            else -> "file://" + this(file)
        }

        return Pair("url", path)
    }
}

class ProjectContext private constructor(private val projectDir: File) : PathContext {
    constructor(project: PProject) : this(project.rootDirectory)
    constructor(project: Project) : this(project.projectDir)

    override fun invoke(file: File): String {
        return simplifyUserHomeDirPath(replacePrefix(file.absolutePath, projectDir.absolutePath.withSlash(), "PROJECT_DIR"))
    }
}

class ModuleContext(private val project: PProject, val module: PModule) : PathContext {
    override fun invoke(file: File): String {
        if (!file.startsWith(project.rootDirectory)) {
            return simplifyUserHomeDirPath(file.absolutePath)
        }

        return "\$MODULE_DIR\$/" + file.toRelativeString(module.moduleFile.parentFile)
    }
}

fun String.withSlash() = if (this.endsWith("/")) this else ("$this/")