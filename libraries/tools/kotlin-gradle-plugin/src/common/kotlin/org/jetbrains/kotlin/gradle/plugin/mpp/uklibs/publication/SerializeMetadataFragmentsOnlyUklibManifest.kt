package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.ATTRIBUTES
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENT_FILES
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.CURRENT_UMANIFEST_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENTS
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENT_IDENTIFIER
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.UMANIFEST_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.UMANIFEST_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment

@DisableCachingByDefault(because = "...")
internal abstract class SerializeMetadataFragmentsOnlyUklibManifest : DefaultTask() {
    @get:Internal
    abstract val metadataFragments: ListProperty<UklibFragment>

    @get:OutputDirectory
    val outputManifest: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("kotlin/uklibs/${project.name}.uklibManifest")
    )

    @TaskAction
    fun run() {
        // FIXME: Check for bamboo has to be somewhere in consumer's GMT
        val fragments = metadataFragments.get()
        val manifest = mapOf(
            FRAGMENTS to fragments.sortedBy {
                // Make sure we have some stable order of fragments
                it.identifier
            }.map {
                mapOf(
                    FRAGMENT_IDENTIFIER to it.identifier,
                    ATTRIBUTES to it.attributes
                        // Make sure we have some stable order of attributes
                        .sorted(),
                    FRAGMENT_FILES to it.files.map { it.path },
                )
            },
            UMANIFEST_VERSION to CURRENT_UMANIFEST_VERSION,
        )
        outputManifest.get().asFile.resolve(UMANIFEST_FILE_NAME).writeText(GsonBuilder().setPrettyPrinting().create().toJson(manifest))
    }
}