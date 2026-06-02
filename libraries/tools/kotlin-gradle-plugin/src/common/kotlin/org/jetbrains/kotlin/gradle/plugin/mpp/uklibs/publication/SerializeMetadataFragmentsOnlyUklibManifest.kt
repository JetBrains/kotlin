package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import kotlinx.serialization.ExperimentalSerializationApi
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.CURRENT_UMANIFEST_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.UMANIFEST_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.UklibManifest
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.UklibManifestFragment
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
    @OptIn(ExperimentalSerializationApi::class)
    fun run() {
        // FIXME: Check for bamboo has to be somewhere in consumer's GMT
        val fragments = metadataFragments.get()
        val manifest = UklibManifest(
            fragments = fragments.sortedBy { it.identifier }.map {
                UklibManifestFragment(
                    identifier = it.identifier,
                    targets = it.attributes.sorted(),
                    files = it.files.map { it.path },
                )
            },
            manifestVersion = CURRENT_UMANIFEST_VERSION,
        )
        outputManifest.get().asFile.resolve(UMANIFEST_FILE_NAME).writeText(
            KgpJson.prettyPrinted.encodeToString(UklibManifest.serializer(), manifest)
        )
    }
}
