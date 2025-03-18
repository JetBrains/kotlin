/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.serializeToZipArchive
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibModule
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics.UklibFragmentsChecker
import org.jetbrains.kotlin.gradle.utils.getFile

@DisableCachingByDefault(because = "This task only compresses Uklib into an archive")
internal abstract class ArchiveUklibTask : DefaultTask() {
    data class ArchiveUklibTaskFragment(
        val fragment: Provider<UklibFragment>,
        val refineesTransitiveClosure: Set<String>,
    )

    // FIXME: @Input?
    // FIXME: Write FT to check the graph that is passed here
    @get:Internal
    abstract val fragments: ListProperty<ArchiveUklibTaskFragment>

    @get:OutputFile
    val outputZip: RegularFileProperty = project.objects.fileProperty().convention(
        // FIXME: use convention name + output locations
        project.layout.buildDirectory.file("library.${Uklib.UKLIB_EXTENSION}")
    )

    @get:Internal
    val temporariesDirectory: DirectoryProperty = project.objects.directoryProperty().convention(
        // FIXME: use streams instead tmp files
        project.layout.buildDirectory.dir("uklibTemp")
    )

    data class UklibWithDuplicateAttributes(
        val duplicates: Set<UklibFragmentsChecker.Violation.DuplicateAttributesFragments>
    ) : IllegalStateException(
        """
            ${Uklib.UKLIB_NAME} can't publish with fragments that have duplicate attributes:
            
            ${duplicates.map { "Attributes \"${it.attributes}\" appear in fragments \"${it.duplicates.map { it.identifier }.sorted()}\""}.joinToString("\n")}
        """.trimIndent()
    )

    // FIXME: Fail if we are trying to publish an empty uklib
    @TaskAction
    fun run() {
        /**
         * Filter out metadata compilations that were skipped. They should be omitted from the umanifest
         *
         * FIXME: What happens when platform compilations are skipped? Write an IT?
         */
        val compiledFragments = fragments.get().filterNot { fragment ->
            val isMetadata = fragment.fragment.get().attributes.count() > 1
            isMetadata && !fragment.fragment.get().file.exists()
        }
        val compiledFragmentIdentifiers = compiledFragments.map { it.fragment.get().identifier }.toSet()

        // Make sure we ended up with fragments that don't have bamboos
        val violations = UklibFragmentsChecker.findViolationsInSourceSetGraph(
            compiledFragments.associate {
                val fragment = it.fragment
                val refinees = it.refineesTransitiveClosure
                UklibFragmentsChecker.FragmentToCheck(
                    fragment.get().identifier,
                    fragment.get().attributes,
                ) to refinees
                    // Filter out bamboos which were avoided by skipped metadata compilations
                    .intersect(compiledFragmentIdentifiers)
            }
        )

        val duplicateAttributes = linkedSetOf<UklibFragmentsChecker.Violation.DuplicateAttributesFragments>()
        violations.forEach {
            when (it) {
                is UklibFragmentsChecker.Violation.DuplicateAttributesFragments -> duplicateAttributes.add(it)
                // We should never get here because these get validated during configuration time at UklibFromKGPModel.kt
                else -> error("FIXME: Unexpected uklib packaging violation report to youtrack: $it")
            }
        }
        if (duplicateAttributes.isNotEmpty()) throw UklibWithDuplicateAttributes(duplicateAttributes)

        Uklib(
            UklibModule(compiledFragments.map { it.fragment.get() }.toSet()),
            Uklib.CURRENT_UMANIFEST_VERSION,
        ).serializeToZipArchive(
            outputZip = outputZip.getFile(),
            temporariesDirectory = temporariesDirectory.getFile(),
        )
    }
}