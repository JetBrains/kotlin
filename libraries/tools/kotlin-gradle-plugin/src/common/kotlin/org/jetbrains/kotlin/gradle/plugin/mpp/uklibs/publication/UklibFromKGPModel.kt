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
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.named
import java.io.File

internal data class KGPUklibFragment(
    val fragment: Provider<UklibFragment>,
    val compilation: KotlinCompilation<*>,
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
                val file = mainCompilation.compileTaskProvider.map { it.klibOutput.get() }
                fragments.add(kgpUklibFragment(mainCompilation, file))
            }
            is KotlinNativeTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                val file = mainCompilation.compileTaskProvider.map { it.klibOutput.get() }
                fragments.add(kgpUklibFragment(mainCompilation, file))
                mainCompilation.cinterops.forEach {
                    fragments.add(
                        kgpUklibFragment(
                            mainCompilation,
                            project.tasks.named<CInteropProcess>(it.interopProcessingTaskName).map {
                                it.klibOutput.get()
                            },
                            mainCompilation.uklibFragmentIdentifier + "_cinterop_${it.name}"
                        )
                    )
                }
            }
            is KotlinJvmTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                @Suppress("UNCHECKED_CAST")
                val jarTask = (project.tasks.named(target.artifactsTaskName) as TaskProvider<Jar>)
                val jarArtifact = jarTask.map { it.archiveFile.get().asFile }
                fragments.add(kgpUklibFragment(mainCompilation, jarArtifact))
            }
            else -> {
                when (val attribute = target.uklibFragmentPlatformAttribute) {
                    is UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndPublishInUmanifest -> { /* Do nothing for AGP */ }
                    is UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndFailOnPublication -> unsupportedTargets.add(attribute.unsupportedTargetName)
                    is UklibFragmentPlatformAttribute.FailOnConsumptionAndPublication,
                    is UklibFragmentPlatformAttribute.ConsumeInPlatformAndMetadataCompilationsAndPublishInUmanifest -> {
                        error(
                            """
                            Trying to publish an invalid Uklib attribute '${attribute}'
                            
                            Please report this issue to YouTrack: https://kotl.in/unexpected-uklib-diagnostic    
                            """.trimIndent()
                        )
                    }
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
        val artifactProvider = metadataCompilation.compileTaskProvider.map {
            metadataCompilation.metadataPublishedArtifacts.singleFile
        }

        fragments.add(
            KGPUklibFragment(
                fragment = artifactProvider.map {
                    UklibFragment(
                        identifier = metadataCompilation.metadataFragmentIdentifier,
                        attributes = metadataCompilation.metadataFragmentAttributes.map {
                            it.convertToStringForPublicationInUmanifest()
                        }.toSet(),
                        files = listOf(it),
                    )
                },
                compilation = metadataCompilation,
            )
        )
    }

    val allPublishedCompilations = publishedMetadataCompilations + uklibPublishedPlatformCompilations
    if (allPublishedCompilations.size > 1) {
        project.ensureSourceSetStructureIsUklibCompliant(allPublishedCompilations)
    } else if (publishedMetadataCompilations.isEmpty() && uklibPublishedPlatformCompilations.size == 1) {
        /**
         * Do not validate anything. Uklib will contain a single platform slice and fragment structure validations don't make sense
         */
    } else if (allPublishedCompilations.isEmpty()) {
        /**
         * Likely no targets were declared
         */
    } else {
        error(
            """
            Encountered unexpected KGP state when trying to create Uklib fragments 
            
            Published compilations: ${allPublishedCompilations}
            Uklib fragments: $fragments
            
            Please report this issue to YouTrack: https://kotl.in/unexpected-uklib-diagnostic
            """.trimIndent()
        )
    }

    return fragments
}

internal suspend fun KotlinMultiplatformExtension.uklibPublishedPlatformCompilations(): List<KotlinCompilation<*>> {
    val allTargets = awaitTargets()
    return allTargets.filterNot {
        it is KotlinMetadataTarget || it.uklibFragmentPlatformAttribute is UklibFragmentPlatformAttribute.ConsumeInMetadataCompilationsAndPublishInUmanifest
    }.map { target ->
        target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
    }
}

private fun kgpUklibFragment(
    mainCompilation: KotlinCompilation<*>,
    fileProvider: Provider<File>,
    fragmentIdentifier: String = mainCompilation.uklibFragmentIdentifier
): KGPUklibFragment {
    val fragmentAttribute = mainCompilation.uklibFragmentPlatformAttribute.convertToStringForPublicationInUmanifest()
    return KGPUklibFragment(
        fragment = fileProvider.map {
            UklibFragment(
                identifier = fragmentIdentifier,
                attributes = setOf(fragmentAttribute),
                files = listOf(it),
            )
        },
        compilation = mainCompilation,
    )
}

/**
 * FIXME: Write FT with necessary KGP source set structures
 *
 * FIXME: Don't do anything if there are no metadata compilations? Isn't this against the idea that everyone must execute and bundle
 * metadata compilations? How does this work with new compilation scheme where we will pass metadata klibs into platform compilations?
 */
private fun Project.ensureSourceSetStructureIsUklibCompliant(publishedCompilations: List<KotlinCompilation<*>>) {
    val publishedFragments = publishedCompilations.flatMap {
        it.internal.allKotlinSourceSets
    }.toSet().associate {
        UklibFragmentsChecker.FragmentToCheck(
            it.name,
            it.metadataFragmentAttributes.map { it.convertToStringForPublicationInUmanifest() }.toSet(),
        ) to it.dependsOn.map {
            it.name
        }.toSet()
    }
    val violations = UklibFragmentsChecker.findViolationsInSourceSetGraph(publishedFragments)

    val sourceSets = project.multiplatformExtension.sourceSets
    violations.forEach {
        when (it) {
            UklibFragmentsChecker.Violation.EmptyRefinementGraph -> error(
                """
                    Refinement graph is unexpectedly empty
                    
                    Please report this issue to YouTrack: https://kotl.in/unexpected-uklib-diagnostic
                """.trimIndent()
            )
            is UklibFragmentsChecker.Violation.MissingFragment -> error(
                """
                    Refinement graph passed to checker is missing a fragment
                    
                    Missing fragment: "${it.missingFragmentIdentifier}"
                    Refinement edges: ${it.refinementEdges}
                    
                    Please report this issue to YouTrack: https://kotl.in/unexpected-uklib-diagnostic
                """.trimIndent()
            )
            is UklibFragmentsChecker.Violation.FragmentWithEmptyAttributes -> error(
                """
                    Fragment "${it.fragment}" is missing attributes
                    
                    Please report this issue to YouTrack: https://kotl.in/unexpected-uklib-diagnostic
                """.trimIndent()
            )
            /**
             * Orphaned intermediate fragment are impossible in the current implementation because we traverse only those source sets that
             * are connected to the metadata compilation and the platform compilations. See orphan test in [UklibFromKGPFragmentsTests]
             */
            is UklibFragmentsChecker.Violation.OrphanedIntermediateFragment -> error(
                """
                    Fragment "${it.fragment}" is an orphan intermediate fragment
                    
                    Please report this issue to YouTrack: https://kotl.in/unexpected-uklib-diagnostic 
                """.trimIndent()
            )
            is UklibFragmentsChecker.Violation.IncompatibleRefinementViolation -> error(
                """
                    Fragment "${it.fragment}" refines unexpected set of fragments: "${it.incompatibleFragments}"
                    
                    Please report this issue to YouTrack: https://kotl.in/unexpected-uklib-diagnostic
                """.trimIndent()
            )
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
                )
            )
        }
    }
}
