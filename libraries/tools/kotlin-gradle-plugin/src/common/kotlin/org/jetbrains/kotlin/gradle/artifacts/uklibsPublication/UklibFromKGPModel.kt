/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts.uklibsPublication

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentAttributes
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.Fragment
import org.jetbrains.kotlin.gradle.artifacts.metadataFragmentIdentifier
import org.jetbrains.kotlin.gradle.artifacts.metadataPublishedArtifacts
import org.jetbrains.kotlin.gradle.artifacts.publishedMetadataCompilations
import org.jetbrains.kotlin.gradle.artifacts.uklibsModel.isSubsetOf
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
import java.io.File


internal data class KGPFragment(
    val fragment: Fragment,
    val providingTask: TaskProvider<*>,
    val outputFile: Provider<File>,
)

internal suspend fun KotlinMultiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments(): List<KGPFragment> {
    val metadataTarget = awaitMetadataTarget()
    val allTargets = awaitTargets()
    // Guarantee that we can safely access any compilations
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    val fragments = mutableListOf<KGPFragment>()

    metadataTarget.publishedMetadataCompilations().forEach { metadataCompilation ->
        val artifact = metadataCompilation.project.provider {
            metadataCompilation.metadataPublishedArtifacts.singleFile
        }
        fragments.add(
            KGPFragment(
                fragment = Fragment(
                    identifier = metadataCompilation.metadataFragmentIdentifier,
                    attributes = metadataCompilation.metadataFragmentAttributes.map { it.unwrap() }.toSet(),
                    file = {
                        artifact.get()
                    }
                ),
                providingTask = metadataCompilation.compileTaskProvider,
                outputFile = artifact,
            )
        )
    }

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
                val file = mainCompilation.compileTaskProvider.flatMap { it.klibOutput }
                fragments.add(
                    KGPFragment(
                        fragment = Fragment(
                            identifier = mainCompilation.fragmentIdentifier,
                            attributes = setOf(mainCompilation.uklibFragmentPlatformAttribute.unwrap()),
                            file = {
                                file.get()
                            }
                        ),
                        providingTask = mainCompilation.compileTaskProvider,
                        outputFile = file,
                    )
                )
            }
            is KotlinNativeTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                val file = mainCompilation.compileTaskProvider.flatMap { it.klibOutput }
                fragments.add(
                    KGPFragment(
                        fragment = Fragment(
                            identifier = mainCompilation.fragmentIdentifier,
                            attributes = setOf(mainCompilation.uklibFragmentPlatformAttribute.unwrap()),
                            file = {
                                file.get()
                            }
                        ),
                        providingTask = mainCompilation.compileTaskProvider,
                        outputFile = file,
                    )
                )
            }
            is KotlinJvmTarget -> {
                val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                @Suppress("UNCHECKED_CAST")
                val jarTask = (target.project.tasks.named(target.artifactsTaskName) as TaskProvider<Jar>)
                val jarArtifact = jarTask.flatMap {
                    it.archiveFile.map { it.asFile }
                }
                fragments.add(
                    KGPFragment(
                        fragment = Fragment(
                            identifier = mainCompilation.fragmentIdentifier,
                            attributes = setOf(mainCompilation.uklibFragmentPlatformAttribute.unwrap()),
                            file = {
                                jarArtifact.get()
                            }
                        ),
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

    return fragments
}

private fun ensureSourceSetStructureIsUklibCompliant(
    publishedCompilations: List<KotlinCompilation<*>>,
    edges: Map<Vertex, Vertex>
) {
    val publishedSourceSets = publishedCompilations.flatMap {
        it.internal.allKotlinSourceSets
    }.toSet()

    val vertices = publishedSourceSets.toSet().map { it.name to Vertex(it.name) }.toMap()
    val edges = publishedSourceSets.flatMap { child ->
        child.dependsOn.map { parent ->
            vertices[child.name]!! to vertices[parent.name]!!
        }
    }.toMap()

    // FIXME: Just promote MultipleSourceSetRootsInCompilationChecker to an error?
    if (publishedSourceSets.isEmpty()) error("???")
    val roots = publishedSourceSets.filter {
        it.dependsOn.isEmpty()
    }
    if (roots.isEmpty()) error("???")
    if (roots.size > 1) error("Source set graph has more than one root")
}

data class VFragment(
    val identifier: String,
    val attributes: Set<String>,
) {
    fun refines(fragment: VFragment): Boolean = attributes.isSubsetOf(fragment.attributes)
}

data class UnderRefinementViolation(
    val fragment: VFragment,
    val underRefinedFragments: Set<VFragment>,
    val actuallyRefinedFragments: Set<VFragment>,
)

data class RefinesIncompatibleFragmentViolation(
    val fragment: VFragment,
    val incompatibleFragments: Set<VFragment>,
)

data class Violations(
    val missingFragments: Set<String> = emptySet(),
    val fragmentsWithEmptyAttributes: Set<VFragment> = emptySet(),
    val cycles: List<List<VFragment>> = emptyList(),
    val duplicateAttributesFragments: Map<Set<String>, List<VFragment>> = emptyMap(),
    val underRefinementViolations: Set<UnderRefinementViolation> = emptySet(),
    val incompatibleRefinementViolations: Set<RefinesIncompatibleFragmentViolation> = emptySet(),
    val orphanedIntermediateFragments: Set<VFragment> = emptySet(),
)

fun checkSourceSetStructure(
    // a dependsOn/refines b
    // FIXME: Accept each fragment as Identifier -> Fragment?
    refinementEdges: Map<VFragment, Set<String>>,
): Violations {
    if (refinementEdges.isEmpty()) error("Refinement graph is empty")

    /**
     * Check that the passed graph has all vertices and the attributes for each vertex are not empty
     */
    val fragments = refinementEdges.keys
    val fragmentByIdentifier = fragments.associateBy { it.identifier }
    val missingFragments = hashSetOf<String>()
    refinementEdges.values.forEach {
        it.forEach {
            // Check all vertices are provided
            if (fragmentByIdentifier[it] == null) {
                missingFragments.add(it)
            }
        }
    }
    if (missingFragments.isNotEmpty()) {
        return Violations(missingFragments = missingFragments)
    }

    // Attributes must not be empty
    val fragmentsWithEmptyAttributes = refinementEdges.keys.filter {
        it.attributes.isEmpty()
    }.toHashSet()
    if (fragmentsWithEmptyAttributes.isNotEmpty()) {
        return Violations(fragmentsWithEmptyAttributes = fragmentsWithEmptyAttributes)
    }

    /**
     * Detect cycles to make sure we are working with a DAG
     */
    val cycles = mutableListOf<List<VFragment>>()
    val caughtInExistingCycle = mutableMapOf<VFragment, List<VFragment>>()
//    val acyclicFragments = setOf<VFragment>()
    // FIXME: This walks over the entire graph for every node
    // Find implementation for https://www.cs.tufts.edu/comp/150GA/homeworks/hw1/Johnson%2075.PDF
    fun findCycles(fragment: VFragment, backtrace: HashSet<VFragment>): Boolean {
//        if (fragment in caughtInExistingCycle) {
//            return true
//            caughtInExistingCycle[fragment]!!
//        }
        if (fragment in backtrace) {
            val cyclePath = backtrace.toList() + listOf(fragment)
            cycles.add(cyclePath)
//            backtrace.forEach {
//                caughtInExistingCycle[it] = cyclePath
//            }
            return true
        }
        backtrace.add(fragment)
        refinementEdges[fragment]!!.forEach {
            val encounteredCycle = findCycles(
                fragmentByIdentifier[it]!!,
                backtrace,
            )
        }
        backtrace.remove(fragment)
        return false
    }
    fragments.forEach {
        findCycles(it, hashSetOf())
    }
    if (cycles.isNotEmpty()) {
        return Violations(cycles = cycles)
    }

    /**
     * Violations of "It's forbidden to have several fragments with the same attributes in uklib"
     *
     * i.e. detect multiple-same targets and bamboos
     */
    val attributeSets: MutableMap<Set<String>, MutableList<VFragment>> = mutableMapOf()
    fragments.forEach {
        attributeSets.getOrPut(it.attributes, { mutableListOf() }).add(it)
    }
    val duplicateAttributesFragments = attributeSets.filter { it.value.size > 1 }

    /**
     * Violations of "Fragment `F1` refines fragment `F2` <=> targets of `F1` are compatible with `F2`"
     *
     * 1. For each fragment using the attributes find which other fragments it must refine
     */
    val expectedRefinementEdges = mutableMapOf<VFragment, MutableSet<VFragment>>()
    fragments.forEach { leftFragment ->
        expectedRefinementEdges[leftFragment] = HashSet()
        fragments.forEach { rightFragment ->
            if (leftFragment != rightFragment && leftFragment.refines(rightFragment)) {
                expectedRefinementEdges[leftFragment]!!.add(rightFragment)
            }
        }
    }

    // Transitive closure of fragment refinees
    val refinementEdgesTransitiveClosure: MutableMap<VFragment, Set<String>> = mutableMapOf()
    fun buildRefinementEdgesTransitiveClosure(fragment: VFragment): Set<String> {
        refinementEdgesTransitiveClosure[fragment]?.let { return it }
        val edges = HashSet<String>(refinementEdges[fragment]!!)
        refinementEdges[fragment]!!.forEach {
            edges.addAll(
                buildRefinementEdgesTransitiveClosure(
                    fragmentByIdentifier[it]!!,
                )
            )
        }
        refinementEdgesTransitiveClosure[fragment] = edges
        return edges
    }
    fragments.forEach { buildRefinementEdgesTransitiveClosure(it) }

    val underRefinementViolations = HashSet<UnderRefinementViolation>()
    val incompatibleRefinementViolations = HashSet<RefinesIncompatibleFragmentViolation>()
    fragments.forEach { fragment ->
        val actuallyRefinedFragments = refinementEdgesTransitiveClosure[fragment]!!.map {
            fragmentByIdentifier[it]!!
        }.toHashSet()
        val expectedRefinementFragments = expectedRefinementEdges[fragment]!!

        /**
         * "Targets of `F1` are compatible with `F2` => Fragment `F1` refines fragment `F2`"
         *
         * 2. Check that the fragment actually refined all the fragments it was supposed to
         */
        val underRefinedFragments = expectedRefinementFragments.subtract(actuallyRefinedFragments)
        if (underRefinedFragments.isNotEmpty()) {
            underRefinementViolations.add(
                UnderRefinementViolation(
                    fragment = fragment,
                    underRefinedFragments = underRefinedFragments,
                    actuallyRefinedFragments = actuallyRefinedFragments,
                )
            )
        }

        /**
         * "Fragment `F1` refines fragment `F2` => Targets of `F1` are compatible with `F2`"
         *
         * 3. Check that the fragment didn't refine any fragments it wasn't compatible with
         */
        val incompatibleFragments = actuallyRefinedFragments.subtract(expectedRefinementFragments)
        if (incompatibleFragments.isNotEmpty()) {
            incompatibleRefinementViolations.add(
                RefinesIncompatibleFragmentViolation(
                    fragment = fragment,
                    incompatibleFragments = incompatibleFragments,
                )
            )
        }
    }

    val orphanedIntermediateFragments = fragments.filter { it.attributes.size > 1 }.toHashSet()
    orphanedIntermediateFragments.removeAll(
        refinementEdges.values.flatten().map {
            fragmentByIdentifier[it]!!
        }.toSet()
    )

    return Violations(
        duplicateAttributesFragments = duplicateAttributesFragments,
        underRefinementViolations = underRefinementViolations,
        incompatibleRefinementViolations = incompatibleRefinementViolations,
        orphanedIntermediateFragments = orphanedIntermediateFragments,
    )
}

data class Vertex(val name: String)
data class Edge(val from: Vertex, val to: Vertex)

internal enum class UklibJsTargetIdentifier {
    js_ir,
    wasm_js,
    wasm_wasi;

    fun deserialize(value: String): UklibJsTargetIdentifier {
        return enumValueOf<UklibJsTargetIdentifier>(value)
    }
}

internal sealed class UklibFragmentPlatformAttribute {
    // Jvm, native, js
    data class PublishAndConsumeInAllCompilations(val attribute: String) : UklibFragmentPlatformAttribute()
    // Android
    data class OnlyConsumeInMetadataCompilationsAndIgnoreAtPublication(val attribute: String) : UklibFragmentPlatformAttribute()
    // External target
    // FIXME: Can we actually ignore consumption for external target? What will happen if uklib resolution sees attributes it doesn't know about
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
            return UklibFragmentPlatformAttribute.OnlyConsumeInMetadataCompilationsAndIgnoreAtPublication(targetName)
        }

        if (this is KotlinMetadataTarget) {
            error("Uklib fragment attribute requested for metadata target")
        }

        when (this) {
            is KotlinNativeTarget -> konanTarget.name
            is KotlinJsIrTarget -> when (platformType) {
                KotlinPlatformType.js -> UklibJsTargetIdentifier.js_ir.name
                KotlinPlatformType.wasm -> when (wasmTargetType ?: error("${KotlinJsIrTarget::class} missing wasm type in wasm platform ")) {
                    KotlinWasmTargetType.JS -> UklibJsTargetIdentifier.wasm_js.name
                    KotlinWasmTargetType.WASI -> UklibJsTargetIdentifier.wasm_wasi.name
                }
                else -> error("${KotlinJsIrTarget::class} unexpected platform type ${platformType}")
            }
            // FIXME: Is this correct?
            is KotlinJvmTarget -> targetName
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