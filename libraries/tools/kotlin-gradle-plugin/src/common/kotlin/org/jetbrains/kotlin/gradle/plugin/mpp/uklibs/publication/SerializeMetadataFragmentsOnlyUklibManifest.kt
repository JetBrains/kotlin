package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.CURRENT_UMANIFEST_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.UMANIFEST_FILE_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.UklibManifest
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.encodeToString

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
        // the fragments stay where the build put them, so the manifest lists their files
        val manifest = UklibManifest.of(metadataFragments.get(), CURRENT_UMANIFEST_VERSION, withFiles = true)
        outputManifest.get().asFile.resolve(UMANIFEST_FILE_NAME).writeText(manifest.encodeToString())
    }
}
