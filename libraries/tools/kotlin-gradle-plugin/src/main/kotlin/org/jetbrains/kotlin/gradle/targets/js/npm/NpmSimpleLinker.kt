/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.stream.JsonWriter
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringWriter
import java.nio.file.Files

/**
 * Handles simple case, when there are no npm depenencies required, but some tasks steel needs
 * node_modules with symlinked packages and packages_imported
 */
class NpmSimpleLinker(val rootProject: Project) {
    private val rootProjectNodeModules = rootProject.nodeJs.root.rootPackageDir.resolve(NpmProject.NODE_MODULES)

    fun link(projects: Collection<NpmProjectPackage>) {
        rootProjectNodeModules.listFiles()?.forEach {
            val path = it.toPath()
            when {
                Files.isSymbolicLink(path) ->
                    Files.delete(path)
                else -> it.deleteRecursively()
            }

        }

        rootProjectNodeModules.mkdirs()

        val fsLinker = createFSLinker()

        // packages
        projects.forEach {
            fsLinker.link(getNodeModulePath(it.npmProject.name), it.npmProject.dir.canonicalFile)
        }

        // packages_imported
        projects.flatMapTo(mutableSetOf()) {
            it.gradleDependencies.externalModules.map { gradleNodeModule -> gradleNodeModule }
        }.forEach {
            fsLinker.link(getNodeModulePath(it.name), it.path.canonicalFile)
        }

        fsLinker.execute()
    }

    private fun getNodeModulePath(name: String) = rootProjectNodeModules.resolve(name).canonicalFile

    interface FSLinker {
        fun link(linkFile: File, target: File)
        fun execute() = Unit
    }

    fun createFSLinker(): FSLinker =
        if (rootProject.nodeJs.root.environment.isWindows) WindowsJunctionFsLinker(rootProject, rootProjectNodeModules)
        else JavaFsLinker()

    class WindowsJunctionFsLinker(val project: Project, val dir: File) : FSLinker {
        val js = StringBuilder()

        init {
            js.appendln("var fs = require('fs')")
        }

        fun addLink(linkFile: String, target: String) {
            js.appendln("fs.symlinkSync(${jsQuotedString(linkFile)}, ${jsQuotedString(target)}, 'junction')")
        }

        private fun jsQuotedString(str: String) = StringWriter().also {
            JsonWriter(it).value(str)
        }.toString()

        override fun link(linkFile: File, target: File) {
            addLink(linkFile.absolutePath, target.absolutePath)
        }

        override fun execute() {
            val result = project.exec { exec ->
                exec.workingDir = dir
                exec.executable = project.nodeJs.root.environment.nodeExecutable
                exec.standardInput = ByteArrayInputStream(js.toString().toByteArray())
                exec.standardOutput = System.`out`
                exec.errorOutput = System.err
            }

            if (result.exitValue != 0) {
                error("Cannot create window junctions: nodejs exited with ${result.exitValue}")
            }
        }
    }

    class JavaFsLinker : FSLinker {
        override fun link(linkFile: File, target: File) {
            Files.createSymbolicLink(linkFile.toPath(), target.toPath())
        }
    }
}