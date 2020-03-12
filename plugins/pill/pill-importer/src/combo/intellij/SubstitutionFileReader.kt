/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.combo.intellij

import com.google.gson.Gson
import org.jetbrains.kotlin.pill.ProjectContext
import java.io.File

class Substitutions(private val substitutions: Map<String, Map<String, List<OrderEntryInfo>>>) {
    fun getForArtifact(artifactName: String): Map<String, List<OrderEntryInfo>>? {
        return substitutions[artifactName]
    }
}

object SubstitutionFileReader {
    private const val NAME_POSTFIX = "-structure-mapping.json"
    private val GSON = Gson()

    fun read(dir: File, ideaProjectDir: File): Substitutions {
        val pathContext = ProjectContext(ideaProjectDir)

        val result = mutableMapOf<String, MutableMap<String, MutableList<OrderEntryInfo>>>()

        for (file in dir.listFiles().orEmpty()) {
            val artifactNames = getArtifactNames(file) ?: continue
            for (artifactName in artifactNames) {
                val substitutions = GSON.fromJson(file.readText(), Array<RawSubstitutionInfo>::class.java)
                for (raw in substitutions) {
                    val substitution = when (raw.type) {
                        "project-library" -> OrderEntryInfo.Library(raw.libraryName ?: error("Library name is not specified"))
                        "module-output" -> OrderEntryInfo.ModuleOutput(raw.moduleName ?: error("Module name is not specified"))
                        "module-library-file" -> {
                            val rawPath = raw.filePath ?: error("Library file path is not specified")
                            val path = pathContext.substituteWithValues(rawPath)
                            val classFile = if (path.startsWith("/")) File(path) else File(ideaProjectDir, path)
                            val libraryInfo = LibraryInfo(classes = listOf(classFile))
                            OrderEntryInfo.ModuleLibrary(libraryInfo)
                        }
                        else -> error("Unexpected substitution kind")
                    }

                    fun register(artifactName: String) {
                        val mapForArtifact = result.getOrPut(artifactName) { mutableMapOf() }
                        val list = mapForArtifact.getOrPut(raw.path) { mutableListOf() }
                        list += substitution
                    }

                    register(artifactName)

                    if (artifactName == "ideaIU") {
                        register("ideaIC")
                    }
                }
            }
        }

        result["intellij-runtime-annotations"] = mutableMapOf(
            "annotations.jar" to mutableListOf<OrderEntryInfo>(OrderEntryInfo.Library("jetbrains-annotations"))
        )

        require(result.isNotEmpty()) { "Substitutions are not found" }

        return Substitutions(result)
    }

    private fun getArtifactNames(file: File): List<String>? {
        val name = file.name
        if (!name.endsWith(NAME_POSTFIX)) {
            return null
        }

        val artifactName = name.dropLast(NAME_POSTFIX.length)
        return when (artifactName) {
            "ideaIU-project" -> listOf("ideaIU", "ideaIC")
            "intellij-core-project" -> listOf("intellij-core")
            "standalone-jps" -> listOf("jps-standalone")
            else -> error("Unknown artifact name $artifactName")
        }
    }
}

private class RawSubstitutionInfo(
    val path: String, val type: String,
    val moduleName: String?,
    val libraryName: String?,
    val filePath: String?
)