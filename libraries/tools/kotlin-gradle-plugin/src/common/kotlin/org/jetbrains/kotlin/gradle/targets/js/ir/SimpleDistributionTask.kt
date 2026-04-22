/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.NODE_MODULES
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

abstract class SimpleDistributionTask : DefaultTask() {

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:InputDirectory
    abstract val mainDirectory: DirectoryProperty

    @get:InputFile
    abstract val importMapLoader: RegularFileProperty

    @get:InputFile
    abstract val importMapFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    private val rootDir: File = project.rootDir

    @TaskAction
    fun distribute() {
        fs.copy { copy ->
            copy.from(mainDirectory) {
                it.exclude("importmap-loader.js")
            }
            copy.from(importMapLoader)
            copy.from(
                parseImportMapModuleDirectories(importMapFile.getFile(), rootDir)
            ) {
                it.includeEmptyDirs = false
                it.into(VENDORS_FOLDER)
                it.eachFile { file ->
                    file.path = File(VENDORS_FOLDER).resolve(file.file.relativeTo(file.file.closestNodeModules())).path
                }
            }
            copy.into(outputDirectory)
        }
    }

    private fun parseImportMapModuleDirectories(importMapFile: File, rootDir: File): Set<File> {
        val importMapContent = importMapFile.readText()
        val importMapObject = JsonParser.parseString(importMapContent).asJsonObject
        val imports = importMapObject.getAsJsonObject("imports") ?: error("No imports in import map $importMapFile")

        return imports.entrySet()
            .map { (_, path) ->
                val relativePath = path.asString.trimStart('/')
                val moduleMainFile = rootDir.resolve(relativePath)
                moduleMainFile.resolveModuleDirectory()
            }.distinct()
            .toSet()
    }

    private fun File.resolveModuleDirectory(): File {
        var packageJsonCandidate = resolveSibling(PACKAGE_JSON)
        while (!packageJsonCandidate.exists()) {
            packageJsonCandidate = packageJsonCandidate.parentFile.resolveSibling(PACKAGE_JSON)
        }

        return packageJsonCandidate.parentFile
    }

    private fun File.closestNodeModules(): File {
        var packageJsonCandidate = this
        while (packageJsonCandidate.name != NODE_MODULES) {
            packageJsonCandidate = packageJsonCandidate.parentFile
        }

        return packageJsonCandidate
    }

    companion object {
        internal const val VENDORS_FOLDER = "vendors"
    }
}
