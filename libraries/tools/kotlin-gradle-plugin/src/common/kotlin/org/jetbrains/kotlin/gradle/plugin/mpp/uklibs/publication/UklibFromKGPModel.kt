/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentIdentifier
import org.jetbrains.kotlin.gradle.artifacts.metadataPublishedArtifacts
import org.jetbrains.kotlin.gradle.artifacts.publishedMetadataCompilations
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics.UklibFragmentsChecker
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import java.io.File

internal data class KGPUklibFragment(
    val fragment: UklibFragment,
    // These must be transitive because we will filter them at execution time for skipped metadata fragments
    val refineesTransitiveClosure: Set<String>,
    val providingTask: TaskProvider<*>,
    val outputFile: Provider<File>,
)

internal suspend fun KotlinMultiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments(): List<KGPUklibFragment> {
    // Guarantee that we can safely access any compilations
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
    val uklibPublishedPlatformCompilations = uklibPublishedPlatformCompilations()

    val fragments = mutableListOf<KGPUklibFragment>()
    val unsupportedTargets = linkedSetOf<String>()
    uklibPublishedPlatformCompilations.forEach { compilation ->
        val target = compilation.target
        /**
         * FIXME: Tie this implementation to the publication implementations that exists in KotlinTargets to make the dependency
         * between the artifact that is published in Uklib and in the old publication model visible
         */
        when (target) {
            is KotlinJsIrTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                val file = mainCompilation.compileTaskProvider.flatMap { it.klibOutput }
                fragments.add(kgpUklibFragment(mainCompilation, mainCompilation.compileTaskProvider, file))
            }
            is KotlinNativeTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                val file = mainCompilation.compileTaskProvider.flatMap { it.klibOutput }
                fragments.add(kgpUklibFragment(mainCompilation, mainCompilation.compileTaskProvider, file))
            }
            is KotlinJvmTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                @Suppress("UNCHECKED_CAST")
                val jarTask = (target.project.tasks.named(target.artifactsTaskName) as TaskProvider<Jar>)
                val jarArtifact = jarTask.flatMap { it.archiveFile.map { it.asFile } }
                fragments.add(kgpUklibFragment(mainCompilation, jarTask, jarArtifact))
            }
            else -> {
                when (val attribute = target.uklibFragmentPlatformAttribute) {
                    is UklibFragmentPlatformAttribute.PublishAndConsumeInMetadataCompilations -> { /* Do nothing for AGP */ }
                    is UklibFragmentPlatformAttribute.PublishAndConsumeInAllCompilations -> { error("Unexpected") }
                    is UklibFragmentPlatformAttribute.FailOnPublicationAndUseTargetNameForMetadataCompilations -> unsupportedTargets.add(attribute.unsupportedTargetName)
                }
            }
        }
    }

    if (unsupportedTargets.isNotEmpty()) {
        unsupportedTargets.forEach {
            project.reportDiagnostic(KotlinToolingDiagnostics.UklibFragmentFromUnexpectedTarget(it))
        }
        // Don't run any additional validations if there are unsupported targets
        return emptyList()
    }

    val publishedMetadataCompilations = awaitMetadataTarget().publishedMetadataCompilations()
    publishedMetadataCompilations.forEach { metadataCompilation ->
        val artifact = metadataCompilation.project.provider {
            metadataCompilation.metadataPublishedArtifacts.singleFile
        }
        unsupportedTargets.addAll(
            metadataCompilation.metadataFragmentAttributes.filterIsInstance<UklibFragmentPlatformAttribute.FailOnPublicationAndUseTargetNameForMetadataCompilations>().map {
                it.unsupportedTargetName
            }
        )

        fragments.add(
            KGPUklibFragment(
                fragment = UklibFragment(
                    identifier = metadataCompilation.metadataFragmentIdentifier,
                    attributes = metadataCompilation.metadataFragmentAttributes.filter {
                        it !is UklibFragmentPlatformAttribute.FailOnPublicationAndUseTargetNameForMetadataCompilations
                    }.map {
                        it.safeToPublish()
                    }.toSet(),
                    file = {
                        artifact.get()
                    }
                ),
                refineesTransitiveClosure = metadataCompilation.refineesTransitiveClosure(),
                providingTask = metadataCompilation.compileTaskProvider,
                outputFile = artifact,
            )
        )
    }

    val allPublishedCompilations = publishedMetadataCompilations + uklibPublishedPlatformCompilations
    if (allPublishedCompilations.isEmpty() || (publishedMetadataCompilations.isEmpty() && uklibPublishedPlatformCompilations.size == 1 && fragments.size == 1)) {
        /**
         * Do not validate anything. Uklib will contain a single platform slice and fragment structure validations don't make sense
         *
         * FIXME: Do we even publish metadataApiElements with a single target?
         */
    } else {
        project.ensureSourceSetStructureIsUklibCompliant(allPublishedCompilations)
    }

    return fragments
}

internal suspend fun KotlinMultiplatformExtension.uklibPublishedPlatformCompilations(): List<KotlinCompilation<*>> {
    val allTargets = awaitTargets()
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
    return allTargets.filterNot {
        it is KotlinMetadataTarget || it.uklibFragmentPlatformAttribute is UklibFragmentPlatformAttribute.PublishAndConsumeInMetadataCompilations
    }.map { target ->
        target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
    }
}

private fun kgpUklibFragment(
    mainCompilation: KotlinCompilation<*>,
    artifactProvidingTask: TaskProvider<*>,
    file: Provider<File>,
) = KGPUklibFragment(
    fragment = UklibFragment(
        identifier = mainCompilation.fragmentIdentifier,
        attributes = setOf(mainCompilation.uklibFragmentPlatformAttribute.safeToPublish()),
        file = {
            file.get()
        }
    ),
    refineesTransitiveClosure = mainCompilation.refineesTransitiveClosure(),
    providingTask = artifactProvidingTask,
    outputFile = file,
)

private fun KotlinCompilation<*>.refineesTransitiveClosure(): Set<String> = internal.allKotlinSourceSets
    .filterNot { it == defaultSourceSet }
    .map { it.metadataFragmentIdentifier }.toSet()

// FIXME: Write FT with necessary KGP source set structures
// FIXME: Don't do anything if there are no metadata compilations? Isn't this against the idea that everyone must execute and bundle metadata compilations?
private fun Project.ensureSourceSetStructureIsUklibCompliant(publishedCompilations: List<KotlinCompilation<*>>) {
    val publishedFragments = publishedCompilations.flatMap {
        it.internal.allKotlinSourceSets
    }.toSet().map {
        UklibFragmentsChecker.FragmentToCheck(
            it.name,
            it.metadataFragmentAttributes.map { it.safeToPublish() }.toSet(),
        ) to it.dependsOn.map {
            it.name
        }.toSet()
    }.toMap()
    val violations = UklibFragmentsChecker.findViolationsInSourceSetGraph(publishedFragments)

    val sourceSets = project.multiplatformExtension.sourceSets
    violations.forEach {
        when (it) {
            UklibFragmentsChecker.Violation.EmptyRefinementGraph -> error("FIXME: Refinement graph is unexpectedly empty, report to youtrack")
            is UklibFragmentsChecker.Violation.MissingFragment -> error("FIXME: Report to youtrack, this is a bug")
            is UklibFragmentsChecker.Violation.FragmentWithEmptyAttributes -> error("FIXME: Report to youtrack, this is a bug")
            is UklibFragmentsChecker.Violation.OrphanedIntermediateFragment -> error("FIXME: Report to youtrack, this is a bug")
            is UklibFragmentsChecker.Violation.IncompatibleRefinementViolation -> error("FIXME: Report to youtrack, this is a bug")
            is UklibFragmentsChecker.Violation.DuplicateAttributesFragments -> {
                /**
                 * We can't validate bamboos at configuration time because metadata compilations are skipped if intermediate source set has
                 * no sources and if we emit an error without taking this into account many projects will not be able to publish
                 */
            }
            is UklibFragmentsChecker.Violation.FirstEncounteredCycle -> project.reportDiagnostic(
                KotlinToolingDiagnostics.CircularDependsOnEdges(it.cycle.map { it.identifier })
            )
            is UklibFragmentsChecker.Violation.UnderRefinementViolation -> project.reportDiagnostic(
                KotlinToolingDiagnostics.UklibSourceSetStructureUnderRefinementViolation(
                    sourceSets.getByName(it.fragment.identifier),
                    it.underRefinedFragments.map { sourceSets.getByName(it.identifier) },
                    it.actuallyRefinedFragments.map { sourceSets.getByName(it.identifier) },
                )
            )
        }
    }
}
