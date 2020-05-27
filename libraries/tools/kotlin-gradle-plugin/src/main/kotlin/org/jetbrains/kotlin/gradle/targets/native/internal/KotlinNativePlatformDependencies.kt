/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_NATIVE_HOME
import org.jetbrains.kotlin.gradle.plugin.compareVersionNumbers
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.targets.metadata.getMetadataCompilationForSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.internal.NativePlatformDependency.*
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import java.io.File

private val Project.isNativeDependencyPropagationEnabled: Boolean
    get() = (findProperty("kotlin.native.enableDependencyPropagation") as? String)?.toBoolean() ?: true

internal fun Project.setUpKotlinNativePlatformDependencies() {
    if (multiplatformExtensionOrNull == null) {
        // not a multiplatform project, nothing to set up
        return
    }

    val kotlinVersion = getKotlinPluginVersion()!!

    // run commonizer only for 1.4+, only for HMPP projects and only on IDE sync
    val allowCommonizer = compareVersionNumbers(kotlinVersion, "1.4") >= 0
            && isKotlinGranularMetadataEnabled
            && !isNativeDependencyPropagationEnabled // temporary fix: turn on commonizer only when native deps propagation is disabled

    val dependencyResolver = NativePlatformDependencyResolver(this, kotlinVersion)

    findSourceSetsToAddDependencies(allowCommonizer).forEach { (sourceSet: KotlinSourceSet, sourceSetDeps: Set<NativePlatformDependency>) ->
        sourceSetDeps.forEach { sourceSetDep: NativePlatformDependency ->
            dependencyResolver.addForResolve(sourceSetDep) { resolvedFiles: Set<File> ->
                //add commonized klibs to metadata compilation
                if (sourceSetDep is CommonizedCommon) {
                    getMetadataCompilationForSourceSet(sourceSet)?.let { compilation ->
                        compilation.compileDependencyFiles += project.files(resolvedFiles)
                    }
                }
                resolvedFiles.forEach { resolvedFile ->
                    dependencies.add(sourceSet.implementationMetadataConfigurationName, dependencies.create(files(resolvedFile)))
                }
            }
        }
    }

    dependencyResolver.resolveAll()
}

private sealed class NativePlatformDependency {
    /* Non-commonized target-neutral libraries taken directly from the Kotlin/Native distribution */
    data class OutOfDistributionCommon(val includeEndorsedLibs: Boolean) : NativePlatformDependency()

    /* Non-commonized libraries for the specific target taken directly from the Kotlin/Native distribution */
    data class OutOfDistributionPlatform(val target: KonanTarget) : NativePlatformDependency()

    /* Commonized (common) libraries */
    data class CommonizedCommon(val targets: Set<KonanTarget>) : NativePlatformDependency()

    /* Commonized libraries for a specific target platform */
    data class CommonizedPlatform(val target: KonanTarget, val common: CommonizedCommon) : NativePlatformDependency()
}

private class NativePlatformDependencyResolver(val project: Project, val kotlinVersion: String) {
    private val distributionDir = File(project.konanHome)
    private val distributionLibsDir = distributionDir.resolve(KONAN_DISTRIBUTION_KLIB_DIR)

    private var alreadyResolved = false
    private val dependencies = mutableMapOf<NativePlatformDependency, MutableList<(Set<File>) -> Unit>>()

    fun addForResolve(dependency: NativePlatformDependency, whenResolved: (Set<File>) -> Unit) {
        check(!alreadyResolved)
        dependencies.computeIfAbsent(dependency) { mutableListOf() }.add(whenResolved)
    }

    fun resolveAll() {
        check(!alreadyResolved)
        alreadyResolved = true

        // first, run commonization
        val targetGroups: List<Pair<CommonizedCommon, Set<KonanTarget>>> = dependencies.keys.filterIsInstance<CommonizedCommon>()
            .map { it to it.targets }

        val commonizedLibsDirs: Map<CommonizedCommon, File> =
            runCommonizerInBulk(
                project = project,
                distributionDir = distributionDir,
                baseDestinationDir = distributionDir.resolve(KONAN_DISTRIBUTION_KLIB_DIR).resolve(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR),
                targetGroups = targetGroups.map { it.second },
                kotlinVersion = kotlinVersion
            ).mapIndexed { index: Int, commonizedLibsDir: File ->
                targetGroups[index].first to commonizedLibsDir
            }.toMap()

        // then, resolve dependencies one by one
        dependencies.forEach { (dependency, actions) ->
            val libs = when (dependency) {
                is OutOfDistributionCommon -> {
                    /* stdlib, endorsed libs */
                    var hasStdlib = false

                    val libs = libsInCommonDir(distributionLibsDir) { dir ->
                        val isStdlib = dir.endsWith(KONAN_STDLIB_NAME)
                        hasStdlib = hasStdlib || isStdlib

                        return@libsInCommonDir isStdlib || dependency.includeEndorsedLibs
                    }

                    if (!hasStdlib) warnAboutMissingNativeStdlib()

                    libs
                }

                is OutOfDistributionPlatform -> {
                    /* platform libs for a specific target */
                    libsInPlatformDir(distributionLibsDir, dependency.target)
                }

                is CommonizedCommon -> {
                    /* commonized platform libs with expect declarations */
                    val commonizedLibsDir = commonizedLibsDirs.getValue(dependency)
                    libsInCommonDir(commonizedLibsDir)
                }

                is CommonizedPlatform -> {
                    /* commonized platform libs with actual declarations */
                    val commonizedLibsDir = commonizedLibsDirs.getValue(dependency.common)
                    libsInPlatformDir(commonizedLibsDir, dependency.target) + libsInCommonDir(commonizedLibsDir)
                }
            }

            actions.forEach { it(libs) }
        }
    }

    private fun warnAboutMissingNativeStdlib() {
        if (!project.hasProperty("kotlin.native.nostdlib")) {
            SingleWarningPerBuild.show(
                project,
                buildString {
                    append(NO_NATIVE_STDLIB_WARNING)
                    if (PropertiesProvider(project).nativeHome != null)
                        append(NO_NATIVE_STDLIB_PROPERTY_WARNING)
                }
            )
        }
    }

    companion object {
        private fun libsInCommonDir(basePath: File, predicate: (File) -> Boolean = { true }) =
            basePath.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).listFiles()?.filter { predicate(it) }?.toSet() ?: emptySet()

        private fun libsInPlatformDir(basePath: File, target: KonanTarget) =
            basePath.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).resolve(target.name).listFiles()?.toSet() ?: emptySet()
    }
}

private fun Project.findSourceSetsToAddDependencies(allowCommonizer: Boolean): Map<KotlinSourceSet, Set<NativePlatformDependency>> {
    val sourceSetsToAddDeps = mutableMapOf<KotlinSourceSet, Set<NativePlatformDependency>>()
    if (allowCommonizer) {
        sourceSetsToAddDeps += findSourceSetsToAddCommonizedPlatformDependencies()
    }

    val compilationsBySourceSets = CompilationSourceSetUtil.compilationsBySourceSets(this)
    val nativeCompilations = compilationsBySourceSets.values.flattenTo(mutableSetOf()).filterIsInstance<KotlinNativeCompilation>()

    nativeCompilations.associate { it.defaultSourceSet to NativeSourceSetDetails(it.konanTarget, it.enableEndorsedLibs) }
        .forEach { (defaultSourceSet, details) ->
            if (defaultSourceSet !in sourceSetsToAddDeps) {
                sourceSetsToAddDeps[defaultSourceSet] = setOf(
                    OutOfDistributionCommon(details.includeEndorsedLibs),
                    OutOfDistributionPlatform(details.target)
                )
            }
        }

    return sourceSetsToAddDeps
}

private fun Project.findSourceSetsToAddCommonizedPlatformDependencies(): Map<KotlinSourceSet, Set<NativePlatformDependency>> {
    val sourceSetsToAddDeps = mutableMapOf<KotlinSourceSet, Set<NativePlatformDependency>>()

    val compilationsBySourceSets = CompilationSourceSetUtil.compilationsBySourceSets(this)
    val nativeCompilations = compilationsBySourceSets.values.flattenTo(mutableSetOf()).filterIsInstance<KotlinNativeCompilation>()

    nativeCompilations.forEach { nativeCompilation ->
        // consider source sets in compilation only one step above the default source set
        // TODO: reconsider this restriction
        val commonSourceSetCandidates = nativeCompilation.defaultSourceSet.dependsOn - nativeCompilation.defaultSourceSet

        commonSourceSetCandidates.forEach sourceSet@{ sourceSet ->
            if (sourceSet.name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
                || sourceSet.name == KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME
            ) {
                // exclude the most common source sets
                return@sourceSet
            }

            if (sourceSet in sourceSetsToAddDeps) {
                // already processed
                return@sourceSet
            }

            val leafSourceSets = mutableMapOf<KotlinSourceSet, NativeSourceSetDetails>()

            for (compilation in compilationsBySourceSets.getValue(sourceSet)) {
                if (compilation !is KotlinNativeCompilation) {
                    // the source set participates in non-native compilation
                    return@sourceSet
                }

                val defaultSourceSet = compilation.defaultSourceSet
                if (defaultSourceSet == sourceSet) {
                    // there is a compilation where the source set is the default source set
                    return@sourceSet
                }

                leafSourceSets[defaultSourceSet] = NativeSourceSetDetails(
                    target = compilation.konanTarget,
                    includeEndorsedLibs = compilation.enableEndorsedLibs
                )
            }

            val allTargets = leafSourceSets.values.mapTo(mutableSetOf()) { it.target }
            if (allTargets.isEmpty())
                return@sourceSet

            val commonizedCommonDep = CommonizedCommon(allTargets)
            val includeEndorsedLibsToCommonSourceSet = leafSourceSets.values.all { it.includeEndorsedLibs }

            // save the dependencies for the current common native source set
            sourceSetsToAddDeps[sourceSet] = setOf(
                OutOfDistributionCommon(includeEndorsedLibsToCommonSourceSet),
                commonizedCommonDep
            )

            // now set the dependencies for leaf native source sets (those that dependsOn the current common native source set)
            leafSourceSets.forEach { (leafSourceSet, details) ->
                val newLeafSourceSetDeps = setOf(
                    OutOfDistributionCommon(details.includeEndorsedLibs),
                    CommonizedPlatform(details.target, commonizedCommonDep)
                )

                /*
                 * It may happen that the leaf source set has already been processed with another common native source set, and
                 * its dependencies have already been evaluated. Then, it's necessary to merge earlier and newly evaluated dependencies
                 * to narrow combination of commonized targets.
                 *
                 * Why? Consider this example: There is `watchos()` shortcut in Gradle DSL that creates few native targets
                 * in HMPP project with the corresponding source set hierarchy:
                 *
                 *                    watchosMain [commonized targets: watchosX86, watchosArm32, watchosArm64]
                 *                   /           \
                 *     watchosX86Main             watchosDeviceMain [commonized targets: watchosArm32, watchosArm64]
                 *                               /                 \
                 *               watchosArm32Main                   watchosArm64Main
                 *
                 * There are two common native source sets that participate in commonization process:
                 *
                 * 1. `watchosMain`. This source set is included into three native compilations for different native targets.
                 *    Thus, it has three commonized targets: watchosX86, watchosArm32 and watchosArm64.
                 *
                 * 2. `watchosDeviceMain`. Two native compilations -> two commonized targets: watchosArm32 and watchosArm64.
                 *
                 * Let's consider some leaf source set that depends on both of them, for instance, `watchosArm64Main`:
                 *
                 * - When `watchosArm64Main` is processed with `watchosMain`, the dependencies are evaluated from `watchosMain`
                 *   viewpoint. This means the following: the leaf source set should get the libraries with `actual` declarations
                 *   produced as a result of commonization of the three targets: watchosX86, watchosArm32 and watchosArm64.
                 *
                 * - When `watchosArm64Main` is processed with `watchosDeviceMain`, which is immediate parent according to the hierarchy,
                 *   the dependencies are evaluated from `watchosDeviceMain` viewpoint. Assuming libraries with `actual` declarations
                 *   produced for tho targets: watchosArm32, watchosArm64.
                 */
                val existingLeafSourceSetDeps = sourceSetsToAddDeps[leafSourceSet]
                sourceSetsToAddDeps[leafSourceSet] = existingLeafSourceSetDeps?.let {
                    mergeLeafSourceSetDeps(leafSourceSet, it, newLeafSourceSetDeps)
                } ?: newLeafSourceSetDeps
            }
        }
    }

    return sourceSetsToAddDeps
}

private fun Project.mergeLeafSourceSetDeps(
    leafSourceSet: KotlinSourceSet,
    existingDeps: Set<NativePlatformDependency>,
    newDeps: Set<NativePlatformDependency>
): Set<NativePlatformDependency> {

    fun merge(dependencies: List<CommonizedPlatform>): CommonizedPlatform? = when (dependencies.size) {
        1 -> dependencies[0]
        2 -> {
            val (first, second) = dependencies
            when {
                first.target != second.target -> {
                    // leaf source set is included into several native compilations for different targets
                    // this is an extremely rare case, though, possible - let's just fallback to no-merge
                    null
                }
                first.common.targets.containsAll(second.common.targets) -> second
                second.common.targets.containsAll(first.common.targets) -> first
                else -> null
            }
        }
        else -> error("Unexpected number of dependencies: $dependencies")
    }

    fun merge(dependencies: List<OutOfDistributionCommon>): OutOfDistributionCommon = when (dependencies.size) {
        1 -> dependencies[0]
        2 -> {
            val (first, second) = dependencies
            if (!first.includeEndorsedLibs) first else second
        }
        else -> error("Unexpected number of dependencies: $dependencies")
    }

    val allDeps = existingDeps + newDeps

    val allDepTypes = allDeps.mapTo(HashSet()) { it::class }
    check(allDepTypes.size == 2 && OutOfDistributionCommon::class in allDepTypes && CommonizedPlatform::class in allDepTypes) {
        // sanity check, such case should not happen normally
        "Unexpected combination of dependency types in leaf $leafSourceSet while merging $existingDeps and $newDeps"
    }

    val result = HashSet<NativePlatformDependency>()
    result += merge(allDeps.filterIsInstance<CommonizedPlatform>()) ?: run {
        logger.warn("Leaf $leafSourceSet is probably included into several native compilations with different targets.")
        return existingDeps
    }
    result += merge(allDeps.filterIsInstance<OutOfDistributionCommon>())

    return result
}

private class NativeSourceSetDetails(
    val target: KonanTarget,
    val includeEndorsedLibs: Boolean
)

internal const val NO_NATIVE_STDLIB_WARNING =
    "The Kotlin/Native distribution used in this build does not provide the standard library. "

internal const val NO_NATIVE_STDLIB_PROPERTY_WARNING =
    "Make sure that the '$KOTLIN_NATIVE_HOME' property points to a valid Kotlin/Native distribution."
