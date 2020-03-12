/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill

import org.gradle.api.Project
import java.io.File

private val USER_HOME_DIR_PATH = System.getProperty("user.home").withoutSlash()
private val MAVEN_REPOSITORY_PATH = File(USER_HOME_DIR_PATH, ".m2/repository").absolutePath

private const val VAR_USER_HOME = "USER_HOME"
private const val VAR_MAVEN_REPOSITORY = "MAVEN_REPOSITORY"
private const val VAR_PROJECT_DIR = "PROJECT_DIR"
private const val VAR_MODULE_DIR = "MODULE_DIR"

private fun String.applyVariable(variableName: String, value: String): String {
    val variableText = "$$variableName$"
    return when {
        this == value -> variableText
        startsWith("$value/") -> variableText + "/" + this.drop(value.length + 1)
        else -> this
    }
}

private fun String.substituteVariable(variableName: String, value: String): String {
    val variableText = "$$variableName$"
    return when {
        this == variableText -> value
        this.startsWith("$variableText/") -> value + "/" + this.drop(variableText.length + 1)
        else -> this
    }
}

private fun String.applyUserHomeDirPath(): String {
    return this.applyVariable(VAR_USER_HOME, USER_HOME_DIR_PATH)
}

private fun String.applyMavenRepositoryPath(): String {
    return this.applyVariable(VAR_MAVEN_REPOSITORY, MAVEN_REPOSITORY_PATH)
}

private fun String.applyProjectDir(projectDir: File): String {
    return this.applyVariable(VAR_PROJECT_DIR, projectDir.absolutePath)
}

private fun String.applyModuleDir(projectDir: File, moduleFile: File): String {
    val file = File(this)
    if (!file.startsWith(projectDir)) {
        return this
    }

    return '$' + VAR_MODULE_DIR + "$/" + file.toRelativeString(moduleFile.parentFile)
}

private fun String.substituteUserHome(): String {
    return this.substituteVariable(VAR_USER_HOME, USER_HOME_DIR_PATH)
}

private fun String.substituteMavenRepository(): String {
    return this.substituteVariable(VAR_MAVEN_REPOSITORY, MAVEN_REPOSITORY_PATH)
}

private fun String.substituteProject(projectDir: File): String {
    return this.substituteVariable(VAR_PROJECT_DIR, projectDir.absolutePath)
}

private fun String.substituteModule(moduleFile: File): String {
    return this.substituteVariable(VAR_MODULE_DIR, moduleFile.parentFile.absolutePath)
}

interface PathContext {
    /** Replaces paths with path variables (/home/user -> USER_HOME). */
    fun substituteWithVariables(file: File): String

    /** Replaces path variables with actual paths (USER_HOME -> /home/user). */
    fun substituteWithValues(path: String): String
}

fun PathContext.getUrlWithVariables(file: File): String {
    val path = this.substituteWithVariables(file)
    return when {
        file.isFile && file.extension.toLowerCase() == "jar" -> "jar://$path!/"
        else -> "file://$path"
    }
}

class ProjectContext(private val projectDir: File) : PathContext {
    constructor(project: PProject) : this(project.rootDirectory)
    constructor(project: Project) : this(project.projectDir)

    override fun substituteWithValues(path: String): String {
        return path
            .substituteProject(projectDir)
            .substituteMavenRepository()
            .substituteUserHome()
    }

    override fun substituteWithVariables(file: File): String {
        return file.absolutePath
            .applyProjectDir(projectDir)
            .applyMavenRepositoryPath()
            .applyUserHomeDirPath()
    }
}

class ModuleContext(private val projectDir: File, private val moduleFile: File) : PathContext {
    constructor(project: PProject, module: PModule) : this(project.rootDirectory, module.moduleFile)

    override fun substituteWithValues(path: String): String {
        return path
            .substituteModule(moduleFile)
            .substituteProject(projectDir)
            .substituteUserHome()
            .substituteMavenRepository()
    }

    override fun substituteWithVariables(file: File): String {
        return file.absolutePath
            .applyModuleDir(projectDir, moduleFile)
            .applyMavenRepositoryPath()
            .applyUserHomeDirPath()
    }
}

fun String.withoutSlash() = if (this.endsWith("/")) this.dropLast(1) else this