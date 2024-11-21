package org.jetbrains.kotlin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.file.Path


data class Uklib<Target>(
    val module: Module<Target>,
    val fragmentToArtifact: Map<String, File>,
) {
    fun serializeUklibToArchive(
        outputZip: File,
        temporariesDirectory: File,
        serializeTarget: Target.() -> String = { this.toString() },
    ) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val manifest = gson.toJson(
            mapOf(
                "identifier" to module.identifier,
                // FIXME: How are we actually going to handle default target hierarchy cause bamboos?
                "fragments" to module.fragments.filter { fragmentToArtifact[it.identifier]!!.exists() }.map {
                    mapOf(
                        "identifier" to it.identifier,
                        "targets" to it.attributes.map(serializeTarget),
                    )
                }
            )
        )
        zipFragments(
            manifest = manifest,
            fragmentToArtifact = fragmentToArtifact.filter { it.value.exists() },
            outputZip = outputZip,
            temporariesDirectory = temporariesDirectory,
        )
    }

    companion object {
        fun deserializeFromArchive(
            archive: File,
            unarchiveDirectory: Path,
        ): Uklib<String> {
            unzip(
                zipFilePath = archive,
                outputFolderPath = unarchiveDirectory.toFile(),
            )
            return deserializeFromDirectory(
                unarchiveDirectory.toFile()
            )
        }

        fun deserializeFromDirectory(directory: File): Uklib<String> {
            val manifest = directory.resolve("umanifest")
            if (!manifest.exists()) error("manifest doesn't exist")
            val gson = Gson()
            val map = gson.fromJson(manifest.readText(), Map::class.java) as Map<String, Any>
            val identifier = map["identifier"] as String
            val fragmentTokens = map["fragments"] as List<Map<String, Any>>

            val fragments = fragmentTokens.map {
                Fragment(
                    identifier = it["identifier"] as String,
                    attributes = (it["targets"] as List<String>).toSet(),
                )
            }.toSet()
            return Uklib(
                module = Module(
                    identifier = identifier,
                    fragments = fragments,
                ),
                fragmentToArtifact = fragments.map {
                    it.identifier to directory.resolve(it.identifier)
                }.toMap()
            )
        }
    }
}
