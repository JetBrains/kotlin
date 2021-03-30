/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.NativeDistributionCommonizerOutputLayout
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.level
import org.jetbrains.kotlin.compilerRunner.GradleCliCommonizer
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.CInteropSettings
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinSourceSetsIncludingDefault
import org.jetbrains.kotlin.gradle.plugin.sources.resolveAllDependsOnSourceSets
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerTask.CInteropGist
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.utils.transitiveClosure
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal open class CInteropCommonizerTask : AbstractCInteropCommonizerTask() {

    internal data class CInteropGist(
        @get:Input val identifier: CInteropIdentifier,
        @get:Input val konanTarget: KonanTarget,
        @get:Internal val sourceSets: Provider<Set<KotlinSourceSet>>,
        @get:Classpath val libraryFile: Provider<File>,
        @get:Classpath val dependencies: FileCollection
    ) {
        @Suppress("unused") // Used for UP-TO-DATE check
        @get:Input
        val allSourceSetNames: Provider<List<String>> = sourceSets.map { it.resolveAllDependsOnSourceSets().map(Any::toString) }
    }

    override val outputDirectory: File = project.buildDir.resolve("classes/kotlin/commonizer")

    @get:Nested
    internal var cinterops = setOf<CInteropGist>()
        private set

    /**
     * All library files produced by the [Project.commonizeNativeDistributionTask] that are relevant for commonization
     */
    @get:Classpath
    internal val nativeDistributionLibraries: Set<File>
        get() {
            val commonizeNativeDistribution = project.commonizeNativeDistributionTask.get()
            return getCommonizationParameters().flatMapTo(mutableSetOf()) { parameters ->
                parameters.commonizerTarget.withAllTransitiveTargets().flatMap { target ->
                    commonizeNativeDistribution.commonizerTargetOutputDirectories.flatMap { outputDirectory ->
                        NativeDistributionCommonizerOutputLayout.getTargetDirectory(outputDirectory, target)
                            .listFiles().orEmpty().toList()
                    }
                }
            }
        }

    @OutputDirectories
    fun getAllOutputDirectories(): Set<File> {
        return getCommonizationParameters().map { outputDirectory(it) }.toSet()
    }

    fun from(vararg tasks: CInteropProcess) = from(
        tasks.toList()
            .onEach { task -> this.dependsOn(task) }
            .map { task -> task.toGist() }
    )

    internal fun from(vararg cinterop: CInteropGist) {
        from(cinterop.toList())
    }

    internal fun from(cinterops: List<CInteropGist>) {
        this.cinterops += cinterops
    }

    fun exclude(vararg tasks: CInteropProcess) {
        exclude(tasks.map { it.settings.identifier })
    }

    fun exclude(vararg settings: CInteropSettings) {
        exclude(settings.mapNotNull { (it as? DefaultCInteropSettings)?.identifier })
    }

    internal fun exclude(interopIdentifiers: List<CInteropIdentifier>) {
        this.cinterops = this.cinterops.filterTo(mutableSetOf()) { it.identifier !in interopIdentifiers }
    }

    @TaskAction
    internal fun commonizeCInteropLibraries() {
        getCommonizationParameters().forEach(::commonize)
    }

    private fun commonize(parameters: CInteropCommonizationParameters) {
        val cinteropsForTarget = cinterops.filter { cinterop -> cinterop.identifier in parameters.interops }
        outputDirectory(parameters).deleteRecursively()
        if (cinteropsForTarget.isEmpty()) return

        GradleCliCommonizer(project).commonizeLibraries(
            konanHome = project.file(project.konanHome),
            outputCommonizerTarget = parameters.commonizerTarget,
            inputLibraries = cinteropsForTarget.map { it.libraryFile.get() }.toSet(),
            dependencyLibraries = cinteropsForTarget.flatMap { it.dependencies.files }.toSet() + nativeDistributionLibraries,
            outputDirectory = outputDirectory(parameters)
        )
    }

    @Nested
    internal fun getCommonizationParameters(): Set<CInteropCommonizationParameters> {
        val sharedNativeCompilations = (project.multiplatformExtensionOrNull ?: return emptySet())
            .targets.flatMap { it.compilations }
            .filterIsInstance<KotlinSharedNativeCompilation>()

        fun getCommonizationParameters(compilation: KotlinSharedNativeCompilation): CInteropCommonizationParameters? {
            return CInteropCommonizationParameters(
                commonizerTarget = project.getCommonizerTarget(compilation) as? SharedCommonizerTarget ?: return null,
                interops = project.getDependingNativeCompilations(compilation)
                    /* If a depending native compilation has no interop, then commonization is useless */
                    .flatMap { nativeCompilation -> nativeCompilation.cinterops.ifEmpty { return null } }
                    .map { interop -> interop.identifier }
                    .toSet()
            )
        }

        return sharedNativeCompilations.mapNotNull(::getCommonizationParameters).toSet()
            .run(::removeNotRegisteredInterops)
            .run(::removeEmptyInterops)
            .run(::removeHierarchicalParameters)
            .run(::removeRedundantParameters)
    }

    override fun getCommonizationParameters(compilation: KotlinSharedNativeCompilation): CInteropCommonizationParameters? {
        val supportedParameters = getCommonizationParameters().filter { parameters -> parameters.supports(compilation) }
        if (supportedParameters.isEmpty()) return null
        assert(supportedParameters.size == 1) {
            "Unnecessary work detected: Multiple commonization parameters seem to be doing redundant work"
        }
        return supportedParameters.first()
    }

    private fun CInteropCommonizationParameters.supports(
        compilation: KotlinSharedNativeCompilation
    ): Boolean {
        val registeredInterops = cinterops.map { it.identifier }
        val commonizerTargetOfCompilation = project.getCommonizerTarget(compilation) ?: return false
        val interopsOfCompilation = project.getDependingNativeCompilations(compilation)
            .flatMap { it.cinterops }.map { it.identifier }
            .filter { interop -> interop in registeredInterops }

        return commonizerTarget.contains(commonizerTargetOfCompilation) && interops.containsAll(interopsOfCompilation)
    }
}

private fun CInteropProcess.toGist(): CInteropGist {
    return CInteropGist(
        identifier = settings.identifier,
        konanTarget = konanTarget,
        sourceSets = project.provider { settings.compilation.kotlinSourceSetsIncludingDefault },
        libraryFile = outputFileProvider,
        dependencies = project.files(project.provider { settings.dependencyFiles })
    )
}

internal data class CInteropCommonizationParameters(
    @get:Input val commonizerTarget: SharedCommonizerTarget,
    @get:Input val interops: Set<CInteropIdentifier>
) : WorkParameters {
    operator fun contains(other: CInteropCommonizationParameters) =
        this.interops.containsAll(other.interops) && this.commonizerTarget.contains(other.commonizerTarget)
}

private fun CInteropCommonizerTask.removeNotRegisteredInterops(
    parameters: Set<CInteropCommonizationParameters>
): Set<CInteropCommonizationParameters> {
    val registeredInterops = this.cinterops.map { it.identifier }
    return parameters.mapTo(mutableSetOf()) { params ->
        params.copy(interops = params.interops.filterTo(mutableSetOf()) { interop -> interop in registeredInterops })
    }
}

private fun removeEmptyInterops(parameters: Set<CInteropCommonizationParameters>): Set<CInteropCommonizationParameters> {
    return parameters.filterTo(mutableSetOf()) { it.interops.isNotEmpty() }
}

private fun removeHierarchicalParameters(parameters: Set<CInteropCommonizationParameters>): Set<CInteropCommonizationParameters> {
    return parameters.filterTo(mutableSetOf()) { it.commonizerTarget.level <= 1 }
}

private fun removeRedundantParameters(parameters: Set<CInteropCommonizationParameters>): Set<CInteropCommonizationParameters> {
    return parameters.filterNotTo(mutableSetOf()) { current ->
        parameters.any { other ->
            other !== current && current in other
        }
    }
}

private operator fun CommonizerTarget.contains(other: CommonizerTarget): Boolean {
    if (this == other) return true
    if (this !is SharedCommonizerTarget) return false
    return targets.any { child -> other in child }
}

private fun SharedCommonizerTarget.withAllTransitiveTargets(): Set<CommonizerTarget> {
    return setOf(this) + this.transitiveClosure<CommonizerTarget> { if (this is SharedCommonizerTarget) this.targets else emptySet() }
}

private fun Project.getDependingNativeCompilations(compilation: KotlinSharedNativeCompilation): Set<KotlinNativeCompilation> {
    /**
     * Some implementations of [KotlinCompilation] do not contain the default source set in
     * [KotlinCompilation.kotlinSourceSets] or [KotlinCompilation.allKotlinSourceSets]
     * see KT-45412
     */
    fun KotlinCompilation<*>.allParticipatingSourceSets(): Set<KotlinSourceSet> {
        return kotlinSourceSetsIncludingDefault + kotlinSourceSetsIncludingDefault.resolveAllDependsOnSourceSets()
    }

    val multiplatformExtension = multiplatformExtensionOrNull ?: return emptySet()
    val allParticipatingSourceSetsOfCompilation = compilation.allParticipatingSourceSets()

    return multiplatformExtension.targets
        .flatMap { target -> target.compilations }
        .filterIsInstance<KotlinNativeCompilation>()
        .filter { nativeCompilation -> nativeCompilation.allParticipatingSourceSets().containsAll(allParticipatingSourceSetsOfCompilation) }
        .toSet()
}
