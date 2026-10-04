/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.NODE_MODULES
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinWebImportMap
import java.io.File
import javax.inject.Inject

/**
 * A custom Gradle task designed to manage the distribution of files for a project,
 * while handling an "import map" configuration to structure dependencies properly.
 * The task consolidates files from a main directory, an import map loader,
 * and dynamically resolved directories specified based on the import map configuration.
 *
 * This task ensures dependencies are organized in a `vendors` directory and maintains
 * relative file paths within the output directory.
 */
@CacheableTask
internal abstract class DistributionWithImportMapTask : DefaultTask() {

    @get:Inject
    internal abstract val fs: FileSystemOperations

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
        val importMap = KotlinWebImportMap.readFrom(importMapFile.get().asFile)
        fs.copy { copy ->
            copy.from(mainDirectory) {
                // mainDirectory contains its own import map, but it has relative import maps.
                // For a distribution we need an import map with absolute paths.
                // That's why we exclude "development" import map, and include another import map with absolute paths
                it.exclude("importmap-loader.js")
            }
            copy.from(importMapLoader)
            copy.from(importMap.resolveAllModuleDirectories(rootDir)) {
                it.includeEmptyDirs = false
                it.into(VENDORS_FOLDER)
                it.eachFile { file ->
                    // For example:
                    // /path/to/project/node_modules/pkg/index.js -> vendors/pkg/index.js
                    // /path/to/project/node_modules/pkg/node_modules/nested-pkg/index.js -> vendors/nested-pkg/index.js
                    val relativePath = file.file.relativeTo(file.file.closestNodeModules())
                    file.path = File(VENDORS_FOLDER).resolve(relativePath).path
                }
            }
            copy.into(outputDirectory)
        }
    }

    private fun File.closestNodeModules(): File {
        var nodeModulesCandidate = this
        while (nodeModulesCandidate.name != NODE_MODULES) {
            nodeModulesCandidate = nodeModulesCandidate.parentFile
        }

        return nodeModulesCandidate
    }

    companion object {
        internal const val VENDORS_FOLDER = "vendors"
    }
}
