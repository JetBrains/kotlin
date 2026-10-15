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
internal fun KonanTarget.swiftPackageDestinationCondition(language: SwiftPackageSourceLanguage): String =
    when (language) {
        SwiftPackageSourceLanguage.SWIFT -> swiftDestinationCondition()
        SwiftPackageSourceLanguage.C_HEADER -> cDestinationCondition()
    }

/*
 * Mac Catalyst also matches `os(iOS)` but has no Kotlin target, so the iOS device condition excludes it.
 */
private fun KonanTarget.swiftDestinationCondition(): String = when (this) {
    KonanTarget.IOS_ARM64 ->
        "os(iOS) && !targetEnvironment(simulator) && !targetEnvironment(macCatalyst) && arch(arm64)"
    KonanTarget.IOS_SIMULATOR_ARM64 ->
        "os(iOS) && targetEnvironment(simulator) && arch(arm64)"
    KonanTarget.IOS_X64 ->
        "os(iOS) && targetEnvironment(simulator) && arch(x86_64)"
    KonanTarget.MACOS_ARM64 ->
        "os(macOS) && arch(arm64)"
    KonanTarget.MACOS_X64 ->
        "os(macOS) && arch(x86_64)"
    KonanTarget.TVOS_ARM64 ->
        "os(tvOS) && !targetEnvironment(simulator) && arch(arm64)"
    KonanTarget.TVOS_SIMULATOR_ARM64 ->
        "os(tvOS) && targetEnvironment(simulator) && arch(arm64)"
    KonanTarget.TVOS_X64 ->
        "os(tvOS) && targetEnvironment(simulator) && arch(x86_64)"
    KonanTarget.WATCHOS_ARM64 ->
        "os(watchOS) && !targetEnvironment(simulator) && arch(arm64_32)"
    KonanTarget.WATCHOS_DEVICE_ARM64 ->
        "os(watchOS) && !targetEnvironment(simulator) && arch(arm64)"
    KonanTarget.WATCHOS_SIMULATOR_ARM64 ->
        "os(watchOS) && targetEnvironment(simulator) && arch(arm64)"
    KonanTarget.WATCHOS_X64 ->
        "os(watchOS) && targetEnvironment(simulator) && arch(x86_64)"
    else -> throw IllegalArgumentException("Swift Export doesn't support $this")
}

/*
 * `__is_target_environment(unknown)` excludes both the simulator and Mac Catalyst.
 */
private fun KonanTarget.cDestinationCondition(): String = when (this) {
    KonanTarget.IOS_ARM64 ->
        "__is_target_os(ios) && __is_target_environment(unknown) && __is_target_arch(arm64)"
    KonanTarget.IOS_SIMULATOR_ARM64 ->
        "__is_target_os(ios) && __is_target_environment(simulator) && __is_target_arch(arm64)"
    KonanTarget.IOS_X64 ->
        "__is_target_os(ios) && __is_target_environment(simulator) && __is_target_arch(x86_64)"
    KonanTarget.MACOS_ARM64 ->
        "__is_target_os(macos) && __is_target_arch(arm64)"
    KonanTarget.MACOS_X64 ->
        "__is_target_os(macos) && __is_target_arch(x86_64)"
    KonanTarget.TVOS_ARM64 ->
        "__is_target_os(tvos) && __is_target_environment(unknown) && __is_target_arch(arm64)"
    KonanTarget.TVOS_SIMULATOR_ARM64 ->
        "__is_target_os(tvos) && __is_target_environment(simulator) && __is_target_arch(arm64)"
    KonanTarget.TVOS_X64 ->
        "__is_target_os(tvos) && __is_target_environment(simulator) && __is_target_arch(x86_64)"
    KonanTarget.WATCHOS_ARM64 ->
        "__is_target_os(watchos) && __is_target_environment(unknown) && __is_target_arch(arm64_32)"
    KonanTarget.WATCHOS_DEVICE_ARM64 ->
        "__is_target_os(watchos) && __is_target_environment(unknown) && __is_target_arch(arm64)"
    KonanTarget.WATCHOS_SIMULATOR_ARM64 ->
        "__is_target_os(watchos) && __is_target_environment(simulator) && __is_target_arch(arm64)"
    KonanTarget.WATCHOS_X64 ->
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
    packageTargets: Set<KonanTarget> = variants.map { it.target }.toSet(),
): String {
    require(variants.isNotEmpty()) { "Nothing to combine" }

    val targetsByContent = LinkedHashMap<String, MutableSet<KonanTarget>>()
    variants.forEach { variant -> targetsByContent.getOrPut(variant.content) { linkedSetOf() }.add(variant.target) }
    if (targetsByContent.values.singleOrNull() == packageTargets) return variants.first().content

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
 * The Swift [modules] of [target].
 */
internal class SwiftExportTargetModules(
    val target: KonanTarget,
    val modules: List<GradleSwiftExportModule>,
)

/**
 * A module of the Swift package combined from several targets.
 */
internal class SwiftPackageModule(
    /** The module from the first target that exports it, with the dependencies it has on any target. */
    val module: GradleSwiftExportModule,
    /** The module from each target that exports it. Empty in the Xcode flow, which copies the sources as they are. */
    val variants: Map<KonanTarget, GradleSwiftExportModule>,
    /** The Apple families each platform-specific dependency is limited to. */
    val dependencyPlatforms: Map<String, Set<Family>>,
)

/**
 * Combines the modules of [targets]: every module that any target exports, with the dependencies it has on any
 * target. Any target's copy of a module can serve as the base, because its kind and bridge name depend only on
 * the module name and the project's bridge prefix.
 */
internal fun combineSwiftExportModules(targets: List<SwiftExportTargetModules>): List<SwiftPackageModule> =
    targets.flatMap { target -> target.modules.map { it.name } }
        .distinct()
        .map { name -> combineVariants(targets.variantsOf(name)) }

/**
 * The module named [name] for every target that exports it.
 */
private fun List<SwiftExportTargetModules>.variantsOf(name: String): Map<KonanTarget, GradleSwiftExportModule> =
    mapNotNull { target -> target.modules.find { it.name == name }?.let { target.target to it } }.toMap()

private fun combineVariants(variants: Map<KonanTarget, GradleSwiftExportModule>): SwiftPackageModule {
    val dependencies = variants.values.flatMap { it.dependencies }.distinct().sorted()
    return SwiftPackageModule(
        module = variants.values.first().withDependencies(dependencies),
        variants = variants,
        dependencyPlatforms = dependencies.mapNotNull { dependency ->
            variants.platformsDependingOn(dependency)?.let { dependency to it }
        }.toMap(),
    )
}

/**
 * The Apple families to limit [dependency] to, or null if it needs no condition. Only the targets that export the
 * module count, as the module is empty on the others.
 */
private fun Map<KonanTarget, GradleSwiftExportModule>.platformsDependingOn(dependency: String): Set<Family>? {
    val (depending, others) = keys.partition { dependency in getValue(it).dependencies }
    if (others.isEmpty()) return null
    val families = depending.map { it.family }.toSet()
    // A condition covers a whole platform, device and simulator alike, so the dependency stays unconditional.
    if (others.any { it.family in families }) return null
    return families
}

private fun GradleSwiftExportModule.withDependencies(dependencies: List<String>): GradleSwiftExportModule = when (this) {
    is GradleSwiftExportModule.BridgesToKotlin -> copy(dependencies = dependencies)
    is GradleSwiftExportModule.SwiftOnly -> copy(dependencies = dependencies)
}
