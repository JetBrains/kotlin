/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

/*
 * Swift Export runs for every Kotlin target, and the Swift API of two targets can differ. A Swift package has
 * one copy of its sources, so a file that differs is combined: the output of each target goes into a
 * conditional compilation branch for the destination of that target. A module that only some targets export
 * is combined the same way, so it compiles to an empty module on the other destinations.
 */

internal enum class SwiftPackageSourceLanguage {
    SWIFT, C_HEADER
}

/**
 * The condition that only holds for the destination of this target.
 *
 * Fails for a target that is not an Apple one.
 */
internal fun KonanTarget.swiftPackageDestinationCondition(language: SwiftPackageSourceLanguage): String {
    val (swift, c) = destinationConditions()
    return when (language) {
        SwiftPackageSourceLanguage.SWIFT -> swift
        SwiftPackageSourceLanguage.C_HEADER -> c
    }
}

/*
 * The Swift and C conditions of every Apple target, read off its target triple. Mac Catalyst is `os(iOS)` too and
 * has no Kotlin target, so the iOS device condition excludes it. In C, `__is_target_environment(unknown)` already
 * rules out both the simulator and Mac Catalyst.
 */
private fun KonanTarget.destinationConditions(): Pair<String, String> = when (this) {
    KonanTarget.IOS_ARM64 ->
        "os(iOS) && !targetEnvironment(simulator) && !targetEnvironment(macCatalyst) && arch(arm64)" to
                "__is_target_os(ios) && __is_target_environment(unknown) && __is_target_arch(arm64)"
    KonanTarget.IOS_SIMULATOR_ARM64 ->
        "os(iOS) && targetEnvironment(simulator) && arch(arm64)" to
                "__is_target_os(ios) && __is_target_environment(simulator) && __is_target_arch(arm64)"
    KonanTarget.IOS_X64 ->
        "os(iOS) && targetEnvironment(simulator) && arch(x86_64)" to
                "__is_target_os(ios) && __is_target_environment(simulator) && __is_target_arch(x86_64)"
    KonanTarget.MACOS_ARM64 ->
        "os(macOS) && arch(arm64)" to
                "__is_target_os(macos) && __is_target_arch(arm64)"
    KonanTarget.MACOS_X64 ->
        "os(macOS) && arch(x86_64)" to
                "__is_target_os(macos) && __is_target_arch(x86_64)"
    KonanTarget.TVOS_ARM64 ->
        "os(tvOS) && !targetEnvironment(simulator) && arch(arm64)" to
                "__is_target_os(tvos) && __is_target_environment(unknown) && __is_target_arch(arm64)"
    KonanTarget.TVOS_SIMULATOR_ARM64 ->
        "os(tvOS) && targetEnvironment(simulator) && arch(arm64)" to
                "__is_target_os(tvos) && __is_target_environment(simulator) && __is_target_arch(arm64)"
    KonanTarget.TVOS_X64 ->
        "os(tvOS) && targetEnvironment(simulator) && arch(x86_64)" to
                "__is_target_os(tvos) && __is_target_environment(simulator) && __is_target_arch(x86_64)"
    KonanTarget.WATCHOS_ARM64 ->
        "os(watchOS) && !targetEnvironment(simulator) && arch(arm64_32)" to
                "__is_target_os(watchos) && __is_target_environment(unknown) && __is_target_arch(arm64_32)"
    KonanTarget.WATCHOS_DEVICE_ARM64 ->
        "os(watchOS) && !targetEnvironment(simulator) && arch(arm64)" to
                "__is_target_os(watchos) && __is_target_environment(unknown) && __is_target_arch(arm64)"
    KonanTarget.WATCHOS_SIMULATOR_ARM64 ->
        "os(watchOS) && targetEnvironment(simulator) && arch(arm64)" to
                "__is_target_os(watchos) && __is_target_environment(simulator) && __is_target_arch(arm64)"
    KonanTarget.WATCHOS_X64 ->
        "os(watchOS) && targetEnvironment(simulator) && arch(x86_64)" to
                "__is_target_os(watchos) && __is_target_environment(simulator) && __is_target_arch(x86_64)"
    else -> throw IllegalArgumentException("Swift Export doesn't support $this")
}

/**
 * The [content] of a generated file for [target].
 */
internal class SwiftExportSourceVariant(val target: KonanTarget, val content: String)

/**
 * Combines the [variants] of a generated file into one file. Targets with the same content share a branch,
 * and content that is the same for all [packageTargets] is returned unchanged.
 *
 * A branch has the whole file with its imports, because an import can be platform specific as well.
 */
internal fun combineSwiftExportSources(
    variants: List<SwiftExportSourceVariant>,
    language: SwiftPackageSourceLanguage,
    packageTargets: Collection<KonanTarget> = variants.map { it.target },
): String {
    require(variants.isNotEmpty()) { "Nothing to combine" }

    val targetsByContent = LinkedHashMap<String, MutableList<KonanTarget>>()
    variants.forEach { variant -> targetsByContent.getOrPut(variant.content) { mutableListOf() }.add(variant.target) }
    if (targetsByContent.size == 1 && variants.size == packageTargets.size) return variants.first().content

    val nextBranch = when (language) {
        SwiftPackageSourceLanguage.SWIFT -> "#elseif"
        SwiftPackageSourceLanguage.C_HEADER -> "#elif"
    }

    return buildString {
        targetsByContent.entries.forEachIndexed { index, (content, targets) ->
            val conditions = targets.map { it.swiftPackageDestinationCondition(language) }
            val condition = conditions.singleOrNull() ?: conditions.joinToString(" || ") { "($it)" }
            appendLine("${if (index == 0) "#if" else nextBranch} $condition")
            appendLine(content.trimEnd('\n'))
        }
        // No #else: a destination no target is built for has no XCFramework slice, and SwiftPM refuses it before
        // compiling anything. A target that doesn't export the module gets no branch, so the file is empty there.
        appendLine("#endif")
    }
}

/**
 * The Swift [modules] of [target], which has [targetName] in the build script.
 */
internal class SwiftExportTargetModules(
    val targetName: String,
    val target: KonanTarget,
    val modules: List<GradleSwiftExportModule>,
)

/**
 * A module of the Swift package combined from several targets.
 */
internal class SwiftPackageModule(
    /** The module of the first target that exports it, with the dependencies it has for any target. */
    val module: GradleSwiftExportModule,
    /** The module of every target that exports it. Empty if the package is generated from one target. */
    val variants: Map<KonanTarget, GradleSwiftExportModule>,
    /** The Apple families a dependency is limited to, for each dependency that only some platforms have. */
    val dependencyPlatforms: Map<String, Set<Family>>,
)

/**
 * Combines the modules of [targets]. The package has every module that any target exports, and every dependency
 * that a module has for any target. A module bridges to Kotlin the same way for every target: its kind and its
 * bridge only depend on its name.
 */
internal fun combineSwiftExportModules(targets: List<SwiftExportTargetModules>): List<SwiftPackageModule> {
    val packageTargets = targets.map { it.target }
    val moduleNames = targets.flatMap { target -> target.modules.map { it.name } }.distinct()
    return moduleNames.map { name ->
        val exportingTargets = targets.mapNotNull { target -> target.modules.find { it.name == name }?.let { target to it } }
        val dependencies = exportingTargets.flatMap { (_, module) -> module.dependencies }.distinct()
        SwiftPackageModule(
            module = exportingTargets.first().second.withDependencies(dependencies),
            variants = exportingTargets.associate { (target, module) -> target.target to module },
            dependencyPlatforms = dependencies.mapNotNull { dependency ->
                val dependingTargets = exportingTargets
                    .filter { (_, module) -> dependency in module.dependencies }
                    .map { (target, _) -> target.target }
                // The module is empty where it isn't exported, so what it always depends on needs no condition.
                if (dependingTargets.size == exportingTargets.size) return@mapNotNull null
                swiftPackagePlatformsOf(dependingTargets, packageTargets)?.let { dependency to it }
            }.toMap(),
        )
    }
}

/**
 * The Apple families that have exactly [targets] among [packageTargets]. Null if a platform has only some of
 * [targets]: a condition can't tell the targets of one platform apart.
 */
private fun swiftPackagePlatformsOf(targets: List<KonanTarget>, packageTargets: List<KonanTarget>): Set<Family>? {
    val families = targets.map { it.family }.toSet()
    if (packageTargets.any { it.family in families && it !in targets }) return null
    return families
}

private fun GradleSwiftExportModule.withDependencies(dependencies: List<String>): GradleSwiftExportModule = when (this) {
    is GradleSwiftExportModule.BridgesToKotlin -> copy(dependencies = dependencies)
    is GradleSwiftExportModule.SwiftOnly -> copy(dependencies = dependencies)
}
