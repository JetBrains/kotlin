package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption

import com.google.gson.Gson
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.ATTRIBUTES
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENTS
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.property

@CacheableTransform
internal abstract class InterprojectUklibManifestToPlatformCompilationTransform :
    TransformAction<InterprojectUklibManifestToPlatformCompilationTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val targetFragmentAttribute: Property<String>
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val jsonPayload = inputArtifact.get().asFile.path.substringAfter("uklibManifest:")
        val json = Gson().fromJson(jsonPayload, Map::class.java) as Map<String, Any>
        val fragments = (json.property<List<Map<String, Any>>>(FRAGMENTS))
        val matchingFragments = fragments.map { fragment ->
            fragment.property<List<String>>(ATTRIBUTES).toSet() to fragment.property<List<String>>(Uklib.CLASSPATH).toSet()
        }.filter {
            it.first.singleOrNull() == parameters.targetFragmentAttribute.get()
        }

        if (matchingFragments.isEmpty()) {
            /**
             * Platform fragment didn't exist in the Uklib. Per lenient interlibrary resolution rules, just return no artifacts for this variant
             */
            return
        }

        if (matchingFragments.size > 1) {
            error("Matched multiple fragments from ${jsonPayload}, but was expecting to find exactly one. Found fragments: $matchingFragments")
        }

        matchingFragments.single().second.forEach {
            // How to determine if this is a directory or a file?
            outputs.dir(it)
        }
        return
    }
}