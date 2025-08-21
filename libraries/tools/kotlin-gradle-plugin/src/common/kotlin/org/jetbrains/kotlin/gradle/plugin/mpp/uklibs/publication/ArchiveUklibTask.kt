/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.serializeToZipArchive
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibModule
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

@DisableCachingByDefault(because = "This task only compresses Uklib into an archive")
internal abstract class ArchiveUklibTask : DefaultTask() {
    @get:Nested
    abstract val fragments: ListProperty<UklibFragment>
    // @Nested doesn't propagate implicit task dependencies: https://github.com/gradle/gradle/issues/13590
    @get:Input
    protected val taskDependencies = fragments.map { "" }

    @get:OutputFile
    val outputZip: RegularFileProperty = project.objects.fileProperty().convention(
        project.layout.buildDirectory.file("kotlin/uklibs/${project.name}.${Uklib.UKLIB_EXTENSION}")
    )

    @get:Internal
    val temporariesDirectory: DirectoryProperty = project.objects.directoryProperty().convention(
        // FIXME: use streams instead tmp files KT-75395
        project.layout.buildDirectory.dir("kotlin/uklibs_tmp")
    )


    data class UklibWithDuplicateAttributes(
        val duplicates: Map<UklibAttributes, Set<FragmentIdentifier>>
    ) : IllegalStateException(
        buildString {
            appendLine("Uklib can't be published with multiple source sets that compile for the same set of targets:")
            duplicates.map { "Target set \"${it.key}\" appears in source sets \"${it.value.sorted()}\""}.forEach {
                appendLine(it)
            }
            appendLine()
            appendLine("Please move all code for each target set into a single source set")
        }
    )

    // FIXME: Fail if we are trying to publish an empty uklib
    @TaskAction
    fun run() {
        /**
         * Filter out metadata compilations that were skipped. They should be omitted from the umanifest
         *
         * FIXME: What happens when platform compilations are skipped? Write an IT?
         */
        val compiledFragments = fragments.get().filter { fragment ->
            val isMetadata = fragment.attributes.count() > 1
            val isASkippedMetadataCompilation = isMetadata && !fragment.file.exists()
            !isASkippedMetadataCompilation
        }

        val bambooFragments = compiledFragments
            .groupBy { it.attributes }
            .filter { it.value.size > 1 }
        if (bambooFragments.isNotEmpty()) throw UklibWithDuplicateAttributes(
            duplicates = bambooFragments.mapValues { it.value.map { it.identifier }.sorted().toSet() }
        )

        outputZip.getFile().parentFile.mkdirs()
        temporariesDirectory.getFile().mkdirs()

        Uklib(
            UklibModule(compiledFragments.toSet()),
            Uklib.CURRENT_UMANIFEST_VERSION,
        ).serializeToZipArchive(
            outputZip = outputZip.getFile(),
            temporariesDirectory = temporariesDirectory.getFile(),
        )
    }
}

private typealias UklibAttributes = Set<String>
private typealias FragmentIdentifier = String