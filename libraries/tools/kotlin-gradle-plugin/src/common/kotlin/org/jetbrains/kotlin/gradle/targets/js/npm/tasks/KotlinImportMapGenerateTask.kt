/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules.Companion.JS_SUFFIX
import org.jetbrains.kotlin.gradle.utils.getFile
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

@DisableCachingByDefault
abstract class KotlinImportMapGenerateTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val installArtifacts: ConfigurableFileCollection

    @get:InputDirectory
    abstract val inputDirectory: DirectoryProperty

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Internal
    abstract val nodeModulesDirectory: DirectoryProperty

    @get:OutputFile
    abstract val importMapFile: RegularFileProperty

    @get:OutputFile
    abstract val importMapLoaderFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val importMap = mutableMapOf<String, String>()

        val nodeModulesDir = nodeModulesDirectory.getFile()

        val modules = NpmProjectModules(
            inputDirectory.getFile(),
            packageJsonEntries = listOf("module", "main"),
            indexFileSuffixes = listOf(JS_SUFFIX, ".mjs")
        )

        collectModuleNames(nodeModulesDir.toPath()).forEach { moduleName ->
            val resolvedPath = modulePath(modules, moduleName)
            importMap[moduleName] = resolvedPath
        }

        val result = mapOf("imports" to importMap)
        val gson = GsonBuilder().setPrettyPrinting().create()
        val resultImportMapFile = importMapFile.getFile()
        resultImportMapFile.writeText(gson.toJson(result))

        importMapLoaderFile.getFile().writeText(
            """
            |const script = document.createElement('script');
            |script.type = 'importmap';
            |script.textContent = JSON.stringify(${gson.toJson(result)});
            |document.currentScript.after(script);
            """.trimMargin()
        )
    }

    private fun collectModuleNames(nodeModulesDir: Path): List<String> {
        if (!nodeModulesDir.isDirectory()) return emptyList()

        return nodeModulesDir.listDirectoryEntries()
            // no follow symlinks because in node_modules we have installed workspaces, we don't need to resolve them
            .filter { it.isDirectory(LinkOption.NOFOLLOW_LINKS) }
            .flatMap { entry ->
                if (entry.name.startsWith("@")) {
                    if (entry.name.startsWith("@types")) return@flatMap emptyList()

                    entry.listDirectoryEntries()
                        .filter { it.isDirectory() }
                        .map { "${entry.name}/${it.name}" }
                } else {
                    listOf(entry.name)
                }
            }
    }

    private fun modulePath(
        modules: NpmProjectModules,
        moduleName: String,
    ): String {
        val resolvedFile = modules.resolve(moduleName) ?: error("Module $moduleName not found")
        val relativePath = resolvedFile.relativeTo(rootDirectory.getFile()).path
        return "/$relativePath"
    }

    companion object {
        const val NAME = "generateImportMap"
    }
}
