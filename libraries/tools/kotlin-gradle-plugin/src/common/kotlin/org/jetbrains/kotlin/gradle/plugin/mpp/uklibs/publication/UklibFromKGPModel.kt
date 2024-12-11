/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentIdentifier
import org.jetbrains.kotlin.gradle.artifacts.metadataPublishedArtifacts
import org.jetbrains.kotlin.gradle.artifacts.publishedMetadataCompilations
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragmentsChecker
import java.io.File


internal data class KGPUklibFragment(
    val fragment: UklibFragment,
    // These must be transitive
    val refineesTransitiveClosure: Set<String>,
    val providingTask: TaskProvider<*>,
    val outputFile: Provider<File>,
)

internal suspend fun KotlinMultiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments(): List<KGPUklibFragment> {
    val metadataTarget = awaitMetadataTarget()
    val allTargets = awaitTargets()
    // Guarantee that we can safely access any compilations
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
    val publishedMetadataCompilations = metadataTarget.publishedMetadataCompilations()

    val fragments = mutableListOf<KGPUklibFragment>()

    publishedMetadataCompilations.forEach { metadataCompilation ->
        val artifact = metadataCompilation.project.provider {
            metadataCompilation.metadataPublishedArtifacts.singleFile
        }
        fragments.add(
            KGPUklibFragment(
                fragment = UklibFragment(
                    identifier = metadataCompilation.metadataFragmentIdentifier,
                    attributes = metadataCompilation.metadataFragmentAttributes.map { it.unwrap() }.toSet(),
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

    val publishedPlatformCompilations = mutableListOf<KotlinCompilation<*>>()
    allTargets.filterNot {
        it == metadataTarget
    }.forEach { target ->
        /**
         * FIXME: Tie this implementation to the publication implementations that are hardcoded in KotlinTarget to make the dependency
         * between the artifact that is published in Uklib and in the old publication model visible
         */
        when (target) {
            is KotlinJsIrTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                publishedPlatformCompilations.add(mainCompilation)
                val file = mainCompilation.compileTaskProvider.flatMap { it.klibOutput }
                fragments.add(
                    KGPUklibFragment(
                        fragment = UklibFragment(
                            identifier = mainCompilation.fragmentIdentifier,
                            attributes = setOf(mainCompilation.uklibFragmentPlatformAttribute.unwrap()),
                            file = {
                                file.get()
                            }
                        ),
                        refineesTransitiveClosure = mainCompilation.refineesTransitiveClosure(),
                        providingTask = mainCompilation.compileTaskProvider,
                        outputFile = file,
                    )
                )
            }
            is KotlinNativeTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                publishedPlatformCompilations.add(mainCompilation)
                val file = mainCompilation.compileTaskProvider.flatMap { it.klibOutput }
                fragments.add(
                    KGPUklibFragment(
                        fragment = UklibFragment(
                            identifier = mainCompilation.fragmentIdentifier,
                            attributes = setOf(mainCompilation.uklibFragmentPlatformAttribute.unwrap()),
                            file = {
                                file.get()
                            }
                        ),
                        refineesTransitiveClosure = mainCompilation.refineesTransitiveClosure(),
                        providingTask = mainCompilation.compileTaskProvider,
                        outputFile = file,
                    )
                )
            }
            is KotlinJvmTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                publishedPlatformCompilations.add(mainCompilation)
                @Suppress("UNCHECKED_CAST")
                val jarTask = (target.project.tasks.named(target.artifactsTaskName) as TaskProvider<Jar>)
                val jarArtifact = jarTask.flatMap {
                    it.archiveFile.map { it.asFile }
                }
                fragments.add(
                    KGPUklibFragment(
                        fragment = UklibFragment(
                            identifier = mainCompilation.fragmentIdentifier,
                            attributes = setOf(mainCompilation.uklibFragmentPlatformAttribute.unwrap()),
                            file = {
                                jarArtifact.get()
                            }
                        ),
                        refineesTransitiveClosure = mainCompilation.refineesTransitiveClosure(),
                        providingTask = jarTask,
                        outputFile = jarArtifact,
                    )
                )
            }
            else -> {
                val attribute = target.uklibFragmentPlatformAttribute
                when (attribute) {
                    is UklibFragmentPlatformAttribute.OnlyConsumeInMetadataCompilationsAndIgnoreAtPublication -> { /* Do nothing for AGP */ }
                    is UklibFragmentPlatformAttribute.PublishAndConsumeInAllCompilations -> { /* FIXME: rewrite the logic above */ }
                    is UklibFragmentPlatformAttribute.FailOnPublicationAndIgnoreForConsumption -> target.project.reportDiagnostic(
                        KotlinToolingDiagnostics.UklibFragmentFromUnexpectedTarget(attribute.error)
                    )
                }
            }
        }
    }

    if (publishedMetadataCompilations.isEmpty() && publishedPlatformCompilations.size == 1 && fragments.size == 1) {
        /**
         * Do not validate anything. Uklib will contain a single platform slice and fragment structure validations don't make sense
         *
         * FIXME: Do we even publish metadataApiElements with a single target?
         */
    } else {
        project.ensureSourceSetStructureIsUklibCompliant(
            publishedMetadataCompilations + publishedPlatformCompilations
        )
    }

    return fragments
}

private fun KotlinCompilation<*>.refineesTransitiveClosure(): Set<String> = internal.allKotlinSourceSets.filterNot {
    it == defaultSourceSet
}.map {
    it.metadataFragmentIdentifier
}.toSet()

// FIXME: Don't do anything if there are no metadata compilations? Isn't this against the idea that everyone must execute and bundle metadata compilations?
private fun Project.ensureSourceSetStructureIsUklibCompliant(
    publishedCompilations: List<KotlinCompilation<*>>,
) {
    val publishedFragments = publishedCompilations.flatMap {
        it.internal.allKotlinSourceSets
    }.toSet().map {
        UklibFragmentsChecker.FragmentToCheck(
            it.name,
            it.uklibFragmentPlatformAttributes,
        ) to it.dependsOn.map {
            it.name
        }.toSet()
    }.toMap()
    val violations = UklibFragmentsChecker.findViolationsInSourceSetGraph(publishedFragments)

    violations.forEach {
        when (it) {
            UklibFragmentsChecker.Violation.EmptyRefinementGraph -> error("FIXME: Refinement graph is unexpectedly empty, report to youtrack")
            is UklibFragmentsChecker.Violation.FirstEncounteredCycle -> error("FIXME: Emit cycle checker fatal, we should never get here irl")
            is UklibFragmentsChecker.Violation.MissingFragment -> error("FIXME: Report to youtrack, this is a bug")
            is UklibFragmentsChecker.Violation.FragmentWithEmptyAttributes -> error("FIXME: Report to youtrack, this is a bug")
            is UklibFragmentsChecker.Violation.DuplicateAttributesFragments -> {
                /**
                 * We can't validate bamboos at configuration time because metadata compilations are skipped if intermediate source set has
                 * no sources and if we emit an error without taking this into account no projects will be able to publish
                 */
            }
            is UklibFragmentsChecker.Violation.IncompatibleRefinementViolation -> project.reportDiagnostic(
                KotlinToolingDiagnostics.UklibSourceSetStructureIncompatibleRefinementViolation()
            )
            is UklibFragmentsChecker.Violation.OrphanedIntermediateFragment -> project.reportDiagnostic(
                KotlinToolingDiagnostics.UklibSourceSetStructureOrphanedIntermediateFragment()
            )
            is UklibFragmentsChecker.Violation.UnderRefinementViolation -> project.reportDiagnostic(
                KotlinToolingDiagnostics.UklibSourceSetStructureUnderRefinementViolation()
            )
        }
    }
}

internal enum class UklibTargetFragmentIdentifier {
    js_ir,
    wasm_js,
    wasm_wasi,
    jvm,
    android;
}

internal sealed class UklibFragmentPlatformAttribute {
    // Jvm, native, js
    data class PublishAndConsumeInAllCompilations(val attribute: String) : UklibFragmentPlatformAttribute()
    // Android
    data class OnlyConsumeInMetadataCompilationsAndIgnoreAtPublication(val attribute: String) : UklibFragmentPlatformAttribute()
    // External target
    // FIXME: Can we actually ignore consumption for external target? What will happen if uklib resolution in GMT sees attributes it doesn't know about
    data class FailOnPublicationAndIgnoreForConsumption(val error: String) : UklibFragmentPlatformAttribute()

    // FIXME: Separate unwrap to consume for publication vs compilation
    fun unwrap(): String = when (this) {
        is PublishAndConsumeInAllCompilations -> attribute
        is OnlyConsumeInMetadataCompilationsAndIgnoreAtPublication -> attribute
        is FailOnPublicationAndIgnoreForConsumption -> error(error)
    }
}

internal val KotlinCompilation<*>.uklibFragmentPlatformAttribute: UklibFragmentPlatformAttribute get() = this.target.uklibFragmentPlatformAttribute
internal val KotlinTarget.uklibFragmentPlatformAttribute: UklibFragmentPlatformAttribute
    get() {
        // FIXME: Actually maybe request jvm transform in Android?
        if (this is KotlinAndroidTarget) {
            return UklibFragmentPlatformAttribute.OnlyConsumeInMetadataCompilationsAndIgnoreAtPublication(
                UklibTargetFragmentIdentifier.android.name
            )
        }

        if (this is KotlinMetadataTarget) {
            error("Uklib fragment attribute requested for metadata target")
        }

        when (this) {
            is KotlinNativeTarget -> konanTarget.name
            is KotlinJsIrTarget -> when (platformType) {
                KotlinPlatformType.js -> UklibTargetFragmentIdentifier.js_ir.name
                KotlinPlatformType.wasm -> when (wasmTargetType ?: error("${KotlinJsIrTarget::class} missing wasm type in wasm platform ")) {
                    KotlinWasmTargetType.JS -> UklibTargetFragmentIdentifier.wasm_js.name
                    KotlinWasmTargetType.WASI -> UklibTargetFragmentIdentifier.wasm_wasi.name
                }
                else -> error("${KotlinJsIrTarget::class} unexpected platform type ${platformType}")
            }
            // FIXME: Is this correct?
            is KotlinJvmTarget -> UklibTargetFragmentIdentifier.jvm.name
            else -> null
        }?.let {
            return UklibFragmentPlatformAttribute.PublishAndConsumeInAllCompilations(it)
        }

        val error = when (this) {
            is DecoratedExternalKotlinTarget -> "external target"
            else -> this.targetName
        }
        return UklibFragmentPlatformAttribute.FailOnPublicationAndIgnoreForConsumption(error)
    }

internal val KotlinCompilation<*>.fragmentIdentifier: String get() = defaultSourceSet.name