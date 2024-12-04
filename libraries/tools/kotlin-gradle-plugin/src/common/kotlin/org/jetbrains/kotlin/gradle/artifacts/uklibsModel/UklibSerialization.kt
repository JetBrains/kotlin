package org.jetbrains.kotlin.gradle.artifacts.uklibsModel

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.zipFragments
import java.io.File


internal data class Uklib(
    val module: Module,
    // FIXME: Use this version for a forward compatibility?
    val manifestVersion: String,
) {
    fun serializeUklibToArchive(
        outputZip: File,
        temporariesDirectory: File,
    ) {
        val manifest = GsonBuilder().setPrettyPrinting().create().toJson(
            mapOf(
                FRAGMENTS to module.fragments.filter {
                    // FIXME: Where was this coming from ???
                    //fragmentIdentifierToArtifact[it.identifier]!!.exists()
                    true
                }.sortedBy {
                    // Make sure we have some stable order of fragments
                    it.identifier
                }.map {
                    mapOf(
                        FRAGMENT_IDENTIFIER to it.identifier,
                        ATTRIBUTES to it.attributes
                            // Make sure we have some stable order of attributes
                            .sorted(),
                    )
                }
            )
        )
        zipFragments(
            manifest = manifest,
            fragmentToArtifact = module.fragments.map {
                it.identifier to it.file()
            }.toMap(),
            outputZip = outputZip,
            temporariesDirectory = temporariesDirectory,
        )
    }

    companion object {
        fun deserializeFromDirectory(directory: File): Uklib {
            val umanifest = directory.resolve(UMANIFEST_FILE_NAME)
            if (!umanifest.exists()) error("Can't deserialize Uklib from ${directory} because ${UMANIFEST_FILE_NAME} doesn't exist")
            val json = Gson().fromJson(umanifest.readText(), Map::class.java) as Map<String, Any>

            val fragments = (json[FRAGMENTS] as List<Map<String, Any>>).map { fragment ->
                val fragmentIdentifier = fragment[FRAGMENT_IDENTIFIER] as String
                Fragment(
                    identifier = fragmentIdentifier,
                    attributes = (fragment[ATTRIBUTES] as List<String>).toSet(),
                    file = {
                        directory.resolve(fragmentIdentifier)
                    }
                )
            }.toHashSet()
            return Uklib(
                module = Module(
                    fragments = fragments,
                ),
                manifestVersion = json[UMANIFEST_VERSION] as String,
            )
        }

        // Use this in diagnostics
        const val UKLIB_NAME = "uklib"

        // This extension has to be stable because we need to filter transitive jars in the transform
        const val UKLIB_EXTENSION = "uklib"

        /**
         * The packaging must be equal to the extension because when resolving POM only components, Gradle prefers the artifact with the
         * <packaging> POM value over the .jar (but falls back to jar if the artifact doesn't exist)
         */
        const val UKLIB_PACKAGING = UKLIB_EXTENSION

        const val UMANIFEST_FILE_NAME = "umanifest"

        const val FRAGMENTS = "fragments"
        const val FRAGMENT_IDENTIFIER = "identifier"
        const val ATTRIBUTES = "targets"
        const val UMANIFEST_VERSION = "manifestVersion"

        const val CURRENT_UMANIFEST_VERSION = "0.0.1"
    }
}