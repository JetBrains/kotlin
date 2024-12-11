/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibModule
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragmentsChecker
import org.jetbrains.kotlin.gradle.utils.getFile

internal abstract class ArchiveUklibTask : DefaultTask() {
    @get:Internal
    abstract val fragmentsWithRefinees: MapProperty<UklibFragment, Set<String>>

    @get:OutputFile
    val outputZip: RegularFileProperty = project.objects.fileProperty().convention(
        project.layout.buildDirectory.file("package.${Uklib.UKLIB_EXTENSION}")
    )

    @get:Internal
    val temporariesDirectory: DirectoryProperty = project.objects.directoryProperty().convention(
        project.layout.buildDirectory.dir("uklibTemp")
    )

    data class UklibWithDuplicateAttributes(
        val duplicates: Set<UklibFragmentsChecker.Violation.DuplicateAttributesFragments>
    ) : IllegalStateException(
        """
            ${Uklib.UKLIB_NAME} is being published with fragments that have duplicate attributes:
            
            ${duplicates.map { "Attributes \"${it.attributes}\" appear in fragments \"${it.duplicates.map { it.identifier }.sorted()}\""}.joinToString("\n")}
        """.trimIndent()
    )

    @TaskAction
    fun run() {
        // Filter out metadata compilations that were skipped
        val withoutSkippedMetadataFragments = this.fragmentsWithRefinees.get().filterKeys { fragment ->
            val isMetadata = fragment.attributes.count() > 1
            if (isMetadata && !fragment.file().exists()) { return@filterKeys false }
            return@filterKeys true
        }
        val filteredOutFragmentIdentifiers = this.fragmentsWithRefinees.get().keys.subtract(
            withoutSkippedMetadataFragments.keys
        ).map { it.identifier }

        // Make sure we ended up with fragments that don't have bamboos
        val violations = UklibFragmentsChecker.findViolationsInSourceSetGraph(
            withoutSkippedMetadataFragments.map {
                UklibFragmentsChecker.FragmentToCheck(
                    it.key.identifier,
                    it.key.attributes,
                ) to it.value.subtract(filteredOutFragmentIdentifiers)
            }.toMap()
        )

        val duplicateAttributes = hashSetOf<UklibFragmentsChecker.Violation.DuplicateAttributesFragments>()
        violations.forEach {
            when (it) {
                is UklibFragmentsChecker.Violation.MissingFragment -> {
                    /* do nothing because these came from non-existent metadata compilations? is filtering above even needed then? */
                }
                is UklibFragmentsChecker.Violation.DuplicateAttributesFragments -> duplicateAttributes.add(it)
                else -> error("FIXME: Unexpected uklib packaging violation report to youtrack: $it")
            }
        }
        if (duplicateAttributes.isNotEmpty()) throw UklibWithDuplicateAttributes(duplicateAttributes)

        Uklib(
            UklibModule(
                withoutSkippedMetadataFragments.keys,
            ),
            Uklib.CURRENT_UMANIFEST_VERSION,
        ).serializeUklibToArchive(
            outputZip = outputZip.getFile(),
            temporariesDirectory = temporariesDirectory.getFile(),
        )
    }
}