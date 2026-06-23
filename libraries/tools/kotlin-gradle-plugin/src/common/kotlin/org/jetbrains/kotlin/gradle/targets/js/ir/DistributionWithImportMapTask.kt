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

/**
 * A custom Gradle task designed to manage the distribution of files for a project,
 * while handling an "import map" configuration to structure dependencies properly.
 * The task consolidates files from a main directory, an import map loader,
 * and dynamically resolved directories specified based on the import map configuration.
 *
 * Properties:
 * - `fs`: Provides access to file system operations.
 * - `mainDirectory`: Input directory containing the primary files to distribute.
 * - `importMapLoader`: Input file representing the import map loader for the project.
 * - `importMapFile`: Input file specifying the import map configuration which lists module dependencies.
 * - `outputDirectory`: Target output directory where the distribution will be generated.
 *
 * This task ensures dependencies are organized in a `vendors` directory and maintains
 * relative file paths within the output directory.
 */
@CacheableTask
abstract class DistributionWithImportMapTask : DefaultTask() {

    @get:Inject
    abstract val fs: FileSystemOperations

    /**
     * Input directory containing the primary files to distribute.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val mainDirectory: DirectoryProperty

    /**
     * Input file representing the import map loader for the project.
     *
     * This file is supposed to be added to index.html
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val importMapLoader: RegularFileProperty

    /**
     * Input file in json format specifying the import map configuration which lists module dependencies.
     */
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val importMapFile: RegularFileProperty

    /**
     * Target output directory where the distribution will be generated.
     */
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
