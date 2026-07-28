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
import org.jetbrains.kotlin.gradle.internal.tasks.ProducesKlib
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.maybeCreateDependencyScope
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly


internal class KotlinArchivePlatformArtifactsTasks(
    val mainCompilationTask: TaskProvider<out ProducesKlib>,
    val cinteropCompilationsTasks: List<TaskProvider<CInteropProcess>>,
)

internal val Project.karConfiguration: Configuration
    get() = configurations.maybeCreateDependencyScope(KarLayout.CONFIGURATION_NAME) {
        attributes.attribute(KarLayout.Attributes.compressionMethod, KarLayout.Attributes.CompressionMethod.XZ)
    }

internal val SetupKarArtifactAction = KotlinProjectSetupCoroutine {
    Stage.AfterFinaliseCompilations.await()

    val extension = project.multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine

    // Fill KAR content for publication
    val packTask = tasks.register(KarLayout.TASK_NAME, PackKotlinArchiveTask::class.java) { task ->
        task.destinationDirectory.set(layout.buildDirectory.dir(KarLayout.PACKING_DIRECTORY))
        task.archiveExtension.set(KarLayout.COMPRESSED_ARTIFACT_EXTENSION)
    }
    for (target in extension.awaitTargets()) {
        packTask.fillKotlinArchiveTargetContent(target)
    }
    for (compilation in extension.metadataTarget.publishedMetadataCompilations()) {
        packTask.fillKotlinArchiveMetadataCompilationContent(compilation)
    }
    packTask.fillKotlinArchivePsmContent(project)
    karConfiguration.apply {
        outgoing.artifact(packTask)
    }

    // Request the required KAR contents for consuming configurations
    for (target in extension.awaitTargets()) {
        target.requestKarPlatformArtifactsForCompilation()
    }
    for (sourceSet in multiplatformExtension.awaitSourceSets()) {
        sourceSet.requestDecompressedKarForMetadataCompilation()
    }

    for (target in extension.awaitTargets()) {
        target.configureTransformActionFromKarToPlatformArtifacts()
        target.configureTransformActionFromKarToResources()
    }
    configureTransformActionFromKarXzToKar()
    configureTransformActionFromKarToPsm()
}


private fun KotlinSourceSet.requestDecompressedKarForMetadataCompilation() {
    internal.resolvableMetadataConfiguration.apply {
        attributes.attribute(KarLayout.Attributes.state, KarLayout.Attributes.State.DECOMPRESSED)
    }
}

private fun KotlinTarget.requestKarPlatformArtifactsForCompilation() {
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
