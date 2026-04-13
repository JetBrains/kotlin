/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
internal abstract class KotlinSimpleDevServerTask
@Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contentDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val port: Property<Int>

    @get:Input
    abstract val host: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val importMapFile: RegularFileProperty

    @get:Internal
    abstract val npmRootDirectory: DirectoryProperty

    @TaskAction
    fun start() {
        val serverPort = port.getOrElse(findFreePort())

        val importMapModuleDirectories = parseImportMapModuleDirectories()

        val workQueue = workerExecutor.processIsolation()

        workQueue.submit(DevServerWorkAction::class.java) { params ->
            params.contentDirectory.set(contentDirectory)
            params.host.set(host)
            params.port.set(serverPort)
            params.npmRootDirectory.set(npmRootDirectory)
            params.importMapModuleDirectories.set(importMapModuleDirectories)
        }
    }

    /**
     * Parses the import map file and returns a mapping from module name to its directory on disk.
     *
     * The import map contains entries like `"moduleName": "node_modules/moduleName/main.js"`.
     * The paths are relative to [npmRootDirectory].
     */
    private fun parseImportMapModuleDirectories(): Set<File> {
        val mapFile = importMapFile.getFile()

        val npmRoot = npmRootDirectory.getFile()
        val importMapContent = mapFile.readText()
        val importMapObject = JsonParser.parseString(importMapContent).asJsonObject
        val imports = importMapObject.getAsJsonObject("imports") ?: error("No imports in import map $mapFile")

        return imports.entrySet()
            .map { (_, path) ->
                val relativePath = path.asString.trimStart('/')
                val moduleMainFile = npmRoot.resolve(relativePath)
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

    fun findFreePort(startPort: Int = 8080): Int {
        var port = startPort
        while (true) {
            try {
                java.net.ServerSocket(port).use { return port }
            } catch (_: Exception) {
                port++
            }
        }
    }
}
