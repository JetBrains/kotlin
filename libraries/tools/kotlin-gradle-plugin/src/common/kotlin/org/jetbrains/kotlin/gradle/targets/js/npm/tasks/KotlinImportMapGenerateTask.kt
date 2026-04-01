/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.utils.getFile

@DisableCachingByDefault
abstract class KotlinImportMapGenerateTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val installArtifacts: ConfigurableFileCollection

    @get:InputFile
    abstract val packageJson: RegularFileProperty

    @get:Internal
    abstract val npmRootDir: DirectoryProperty

    @get:OutputFile
    abstract val importMapFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val importMap = mutableMapOf<String, String>()

        val packageJsonFile = packageJson.getFile()
        val inputDirectory = packageJsonFile.parentFile

        val modules = NpmProjectModules(inputDirectory)

        val packageJsonContent = packageJsonFile.readText()
        val packageJsonObject = JsonParser.parseString(packageJsonContent).asJsonObject
        val dependencies = packageJsonObject.getAsJsonObject("dependencies") ?: JsonObject()

        dependencies.keySet().forEach { moduleName ->
            importMap[moduleName] = modulePath(modules, moduleName)
        }

        val result = mapOf("imports" to importMap)
        val gson = GsonBuilder().setPrettyPrinting().create()
        importMapFile.asFile.get().writeText(gson.toJson(result))
    }

    private fun modulePath(
        modules: NpmProjectModules,
        moduleName: String,
    ): String {
        val resolvedFile = modules.resolve(moduleName) ?: error("Module $moduleName not found")
        val relativePath = resolvedFile.relativeTo(npmRootDir.getFile()).path
        return "/$relativePath"
    }
}
