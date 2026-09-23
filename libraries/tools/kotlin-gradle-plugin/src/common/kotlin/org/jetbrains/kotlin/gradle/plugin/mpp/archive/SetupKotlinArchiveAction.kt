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
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
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
        task.incompleteArchiveAllowed.set(project.kotlinPropertiesProvider.allowIncompleteKotlinArchivePublication)
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
    for (sourceSet in extension.awaitSourceSets()) {
        /**
         * We are also doing this for platform source-sets. While they are not using metadata configuration
         * to compile, it still exists, and can be requested for example by IDE import.
         */
        sourceSet.requestDecompressedKarForResolvableMetadataConfiguration()
    }

    configureTransformActionFromKarXzToKar()
    configureTransformActionFromKarToPsm()
}


/**
 * The only consumer of resolvableMetadataConfiguration is [org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation],
 * they work on top of zip archive. We can potentially extract only metadata directory into a separate archive,
 * but that would just be additional work, so we directly pass DECOMPRESSED to the task.
 */
private fun KotlinSourceSet.requestDecompressedKarForResolvableMetadataConfiguration() {
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

/**
 * This code tries to handle the case, where you have both "library-platform" in old publication format
 * and "library" in kar format in your configuration. In that case we need to remove older variant.
 *
 * [defaultKotlinUsageContextWithArtifactsMaybeReplacedByTask] sets both current module and platform module
 * capabilities on Kotlin Archive platform variant, which creates capabilities conflict, resolved by this rule.
 *
 * Unfortunately, as old one didn't have any special capabilities we can't distinguish detect it from unrelated conflict.
 * We use the following heuristic to check if it's indeed KAR:
 * - Both should have the same group
 * - Both should have the same variant name (e.g. `iosArm64ApiElements-published`)
 *   - As additional safety measure to not detect something irrelevant this variant name should have `-published` suffix,
 *     as we know that our variants always have it.
 * - Module name of old component is same as capability name
 * - Module name of new component is same as capability name without [platformSuffix] (as it's root publication component)
 * - There should be exactly 2 of them (legacy and kar)
 *   - If we have several legacy vs several kar versions all except newest is already filtered out.
 *
 * If all our heuristic checks matched, we're removing older of 2 versions, assuming that indeed was platform and root version
 * of the same library.
 */
private fun Configuration.selectNewKotlinArchiveComponentOnLegacyPublicationCapabilityConflict(platformSuffix: String) {
    resolutionStrategy.capabilitiesResolution.all { details ->
        if (details.candidates.size != 2) return@all

        val requestedCapability = details.capability
        val legacyPublicationCandidate = details.candidates.singleOrNull { candidate ->
            val candidateComponent = candidate.id as? ModuleComponentIdentifier ?: return@singleOrNull false
            candidateComponent.group == requestedCapability.group && candidateComponent.module == requestedCapability.name
        } ?: return@all

        val replacementCandidate = details.candidates.singleOrNull { candidate ->
            val candidateComponent = candidate.id as? ModuleComponentIdentifier ?: return@singleOrNull false
            candidateComponent.group == requestedCapability.group && "${candidateComponent.module}-$platformSuffix" == requestedCapability.name
        } ?: return@all

        if (legacyPublicationCandidate.variantName != replacementCandidate.variantName) return@all
        if (!replacementCandidate.variantName.endsWith("-published")) return@all

        details.selectHighestVersion()
            .because("Kotlin Archive ${replacementCandidate.id.displayName} represents the same library as ${legacyPublicationCandidate.id.displayName}")
    }
}
