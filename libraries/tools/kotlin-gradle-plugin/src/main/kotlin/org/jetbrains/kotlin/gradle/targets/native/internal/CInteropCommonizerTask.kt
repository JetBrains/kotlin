/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.commonizer.CommonizerDependency
import org.jetbrains.kotlin.commonizer.TargetedCommonizerDependency
import org.jetbrains.kotlin.commonizer.allLeaves
import org.jetbrains.kotlin.compilerRunner.GradleCliCommonizer
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinSourceSetsIncludingDefault
import org.jetbrains.kotlin.gradle.plugin.sources.withDependsOnClosure
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerTask.CInteropGist
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

@CacheableTask
internal open class CInteropCommonizerTask : AbstractCInteropCommonizerTask() {

    internal data class CInteropGist(
        @get:Input val identifier: CInteropIdentifier,
        @get:Input val konanTarget: KonanTarget,
        @get:Internal val sourceSets: Provider<Set<KotlinSourceSet>>,
        @get:Classpath val libraryFile: Provider<File>
    ) {
        @Suppress("unused") // Used for UP-TO-DATE check
        @get:Input
        val allSourceSetNames: Provider<List<String>> = sourceSets
            .map { it.withDependsOnClosure.map(Any::toString) }
    }

    override val outputDirectory: File = project.buildDir.resolve("classes/kotlin/commonizer")

    @get:Nested
    internal var cinterops = setOf<CInteropGist>()
        private set

    @get:OutputDirectories
    val allOutputDirectories: Set<File>
        get() = getAllInteropsGroups().map { outputDirectory(it) }.toSet()

    @Suppress("unused") // Used for UP-TO-DATE check
    @get:Classpath
    val commonizedNativeDistributionDependencies: Set<File>
        get() = getAllInteropsGroups().flatMap { group -> group.targets }
            .flatMap { target -> project.getNativeDistributionDependencies(target) }
            .toSet()

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

    @TaskAction
    protected fun commonizeCInteropLibraries() {
        getAllInteropsGroups().forEach(::commonize)
    }

    private fun commonize(group: CInteropCommonizerGroup) {
        val cinteropsForTarget = cinterops.filter { cinterop -> cinterop.identifier in group.interops }
        outputDirectory(group).deleteRecursively()
        if (cinteropsForTarget.isEmpty()) return

        GradleCliCommonizer(project).commonizeLibraries(
            konanHome = project.file(project.konanHome),
            outputTargets = group.targets,
            inputLibraries = cinteropsForTarget.map { it.libraryFile.get() }.filter { it.exists() }.toSet(),
            dependencyLibraries = getNativeDistributionDependencies(group),
            outputDirectory = outputDirectory(group),
            logLevel = project.commonizerLogLevel,
            additionalSettings = project.additionalCommonizerSettings,
        )
    }

    private fun getNativeDistributionDependencies(group: CInteropCommonizerGroup): Set<CommonizerDependency> {
        return (group.targets + group.targets.allLeaves()).flatMapTo(mutableSetOf()) { target ->
            project.getNativeDistributionDependencies(target).map { dependency -> TargetedCommonizerDependency(target, dependency) }
        }
    }

    @Nested
    internal fun getAllInteropsGroups(): Set<CInteropCommonizerGroup> {
        val dependents = getAllDependents()
        val allScopeSets = dependents.map { it.scopes }.toSet()
        val rootScopeSets = allScopeSets.filter { scopeSet ->
            allScopeSets.none { otherScopeSet -> otherScopeSet != scopeSet && otherScopeSet.containsAll(scopeSet) }
        }

        return rootScopeSets.map { scopeSet ->
            val dependentsForScopes = dependents.filter { dependent ->
                scopeSet.containsAll(dependent.scopes)
            }

            CInteropCommonizerGroup(
                targets = dependentsForScopes.map { it.target }.toSet(),
                interops = dependentsForScopes.flatMap { it.interops }.toSet()
            )
        }.toSet()
    }

    override fun findInteropsGroup(dependent: CInteropCommonizerDependent): CInteropCommonizerGroup? {
        val suitableGroups = getAllInteropsGroups().filter { group ->
            group.interops.containsAll(dependent.interops) && group.targets.contains(dependent.target)
        }

        assert(suitableGroups.size <= 1) {
            "CInteropCommonizerTask: Unnecessary work detected: More than one suitable group found for cinterop dependent."
        }

        return suitableGroups.firstOrNull()
    }

    @Internal
    internal fun getAllDependents(): Set<CInteropCommonizerDependent> {
        val multiplatformExtension = project.multiplatformExtensionOrNull ?: return emptySet()

        val fromSharedNativeCompilations = multiplatformExtension
            .targets.flatMap { target -> target.compilations }
            .filterIsInstance<KotlinSharedNativeCompilation>()
            .mapNotNull { compilation -> CInteropCommonizerDependent.from(compilation) }
            .toSet()

        val fromSourceSets = multiplatformExtension.sourceSets
            .mapNotNull { sourceSet -> CInteropCommonizerDependent.from(project, sourceSet) }
            .toSet()

        val fromSourceSetsAssociateCompilations = multiplatformExtension.sourceSets
            .mapNotNull { sourceSet -> CInteropCommonizerDependent.fromAssociateCompilations(project, sourceSet) }
            .toSet()

        return (fromSharedNativeCompilations + fromSourceSets + fromSourceSetsAssociateCompilations)
    }
}

private fun CInteropProcess.toGist(): CInteropGist {
    return CInteropGist(
        identifier = settings.identifier,
        konanTarget = konanTarget,
        // FIXME support cinterop with PM20
        sourceSets = project.provider { (settings.compilation as? KotlinCompilation<*>)?.kotlinSourceSetsIncludingDefault },
        libraryFile = outputFileProvider
    )
}
