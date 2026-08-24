/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.artifacts.publishedMetadataCompilations
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.archivesName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal val Project.karPackTask: TaskProvider<PackKotlinArchiveTask>
    get() = project.locateOrRegisterTask<PackKotlinArchiveTask>(KarLayout.PACK_TASK_NAME)

internal val Project.karAssembleTask: TaskProvider<AssembleKotlinArchiveTask>
    get() = project.locateOrRegisterTask<AssembleKotlinArchiveTask>(KarLayout.ASSEMBLE_TASK_NAME)


internal val SetupKotlinArchiveAction = KotlinProjectSetupCoroutine {
    Stage.AfterFinaliseCompilations.await()

    val extension = project.multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine

    val kotlinPublicationFormatProvider = extension.publishing.publicationFormat

    val assembleTask = karAssembleTask
    assembleTask.configure { task ->
        task.outputDirectory.set(layout.buildDirectory.dir(KarLayout.ASSEMBLE_DIRECTORY))
        task.onlyIf { kotlinPublicationFormatProvider.get() == KotlinPublicationFormat.KOTLIN_ARCHIVE }
    }
    for (target in extension.awaitTargets()) {
        assembleTask.fillKotlinArchiveTargetContent(target)
    }
    for (compilation in extension.metadataTarget.publishedMetadataCompilations()) {
        assembleTask.fillKotlinArchiveMetadataCompilationContent(compilation)
    }
    assembleTask.fillKotlinArchivePsmContent(project)

    karPackTask.configure { task ->
        task.assembledKarDirectory.set(assembleTask.flatMap { it.outputDirectory })
        task.outputFile.set(
            layout.buildDirectory.file(project.archivesName.map { archivesName ->
                "${KarLayout.PACKING_DIRECTORY}/$archivesName.${KarLayout.KAR_XZ_PACKED_EXTENSION}"
            })
        )
        task.onlyIf { kotlinPublicationFormatProvider.get() == KotlinPublicationFormat.KOTLIN_ARCHIVE }
    }


    for (target in extension.awaitTargets()) {
        target.requestKarPlatformArtifactsForCompilation()
        target.configureTransformActionFromKarToPlatformArtifacts()
        target.configureTransformActionFromKarToResources()
    }
    for (sourceSet in multiplatformExtension.awaitSourceSets()) {
        // TODO: check if this is okey to do on non-shared source-sets.
        sourceSet.requestDecompressedKarForMetadataCompilation()
    }

    configureTransformActionFromKarXzToKar()
    configureTransformActionFromKarToPsm()
}


/**
 * The only consumer of resolvableMetadataConfiguration is [GranularMetadataTransformation],
 * they work on top of zip archive. We can potentially extract only metadata directory into a separate archive,
 * but that would just be additional work, so we directly pass DECOMPRESSED to the task.
 */
private fun KotlinSourceSet.requestDecompressedKarForMetadataCompilation() {
    internal.resolvableMetadataConfiguration.apply {
        attributes.attribute(KarLayout.Attributes.state, KarLayout.Attributes.State.DECOMPRESSED)
    }
}

private fun KotlinTarget.requestKarPlatformArtifactsForCompilation() {
    if (this !is KotlinTargetWithKotlinArchiveSupport) return
    compilations.configureEach { compilation ->
        val configurations = compilation.internal.configurations

        configurations.compileDependencyConfiguration.apply {
            attributes.attribute(KarLayout.Attributes.state, KarLayout.Attributes.State.PLATFORM_ARTIFACTS_EXTRACTED)
            selectNewKotlinArchiveComponentOnLegacyPublicationCapabilityConflict(targetName.toLowerCaseAsciiOnly())
        }

        configurations.runtimeDependencyConfiguration?.apply {
            attributes.attribute(KarLayout.Attributes.state, KarLayout.Attributes.State.PLATFORM_ARTIFACTS_EXTRACTED)
            selectNewKotlinArchiveComponentOnLegacyPublicationCapabilityConflict(targetName.toLowerCaseAsciiOnly())
        }
    }
}

private fun Configuration.selectNewKotlinArchiveComponentOnLegacyPublicationCapabilityConflict(suffix: String = "") {
    resolutionStrategy.capabilitiesResolution.all { details ->
        if (details.candidates.size != 2) return@all

        val capability = details.capability
        val legacyPublicationCandidate = details.candidates.singleOrNull { candidate ->
            val component = candidate.id as? ModuleComponentIdentifier ?: return@singleOrNull false
            component.group == capability.group && component.module == capability.name
        } ?: return@all

        val replacementCandidate = details.candidates.singleOrNull { candidate ->
            val component = candidate.id as? ModuleComponentIdentifier ?: return@singleOrNull false
            component.group == capability.group && "${component.module}-$suffix" == capability.name
        } ?: return@all

        if (legacyPublicationCandidate.variantName != replacementCandidate.variantName) return@all
        if (!replacementCandidate.variantName.endsWith("-published")) return@all

        details.selectHighestVersion().because("prefer the newer Kotlin Archive component over its legacy target publication")
    }
}
