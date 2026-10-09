/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleArchitecture
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.swiftPackagePlatformNames
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
 * The `<TargetConditionals.h>` macros of the Apple families.
 */
private val targetConditionalsOsMacros: Map<Family, String> = mapOf(
    Family.IOS to "TARGET_OS_IOS",
    Family.OSX to "TARGET_OS_OSX",
    Family.TVOS to "TARGET_OS_TV",
    Family.WATCHOS to "TARGET_OS_WATCH",
)

/**
 * The condition that only holds for the destination of this target.
 *
 * Fails for a target that is not an Apple one.
 */
internal fun KonanTarget.swiftPackageDestinationCondition(language: SwiftPackageSourceLanguage): String {
    val architecture = appleArchitecture
    val isSimulator = when (appleTarget) {
        AppleTarget.IPHONE_SIMULATOR, AppleTarget.TVOS_SIMULATOR, AppleTarget.WATCHOS_SIMULATOR -> true
        AppleTarget.IPHONE_DEVICE, AppleTarget.TVOS_DEVICE, AppleTarget.WATCHOS_DEVICE, AppleTarget.MACOS_DEVICE -> false
    }

    val parts = when (language) {
        SwiftPackageSourceLanguage.SWIFT -> listOfNotNull(
            "os(${swiftPackagePlatformNames.getValue(family)})",
            when {
                family == Family.OSX -> null
                isSimulator -> "targetEnvironment(simulator)"
                else -> "!targetEnvironment(simulator)"
            },
            // Mac Catalyst is `os(iOS)` too, and there is no Kotlin target for it.
            "!targetEnvironment(macCatalyst)".takeIf { family == Family.IOS && !isSimulator },
            "arch(${architecture.clangArch})",
        )
        SwiftPackageSourceLanguage.C_HEADER -> listOfNotNull(
            targetConditionalsOsMacros.getValue(family),
            when {
                family == Family.OSX -> null
                isSimulator -> "TARGET_OS_SIMULATOR"
                else -> "!TARGET_OS_SIMULATOR"
            },
            "!TARGET_OS_MACCATALYST".takeIf { family == Family.IOS && !isSimulator },
            when (architecture) {
                // TARGET_CPU_ARM64 is set for arm64_32 as well.
                AppleArchitecture.ARM64 -> "TARGET_CPU_ARM64 && TARGET_RT_64_BIT"
                AppleArchitecture.ARM64_32 -> "TARGET_CPU_ARM64 && !TARGET_RT_64_BIT"
                AppleArchitecture.X86_64 -> "TARGET_CPU_X86_64"
            },
        )
    }
    return parts.joinToString(" && ")
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
        if (language == SwiftPackageSourceLanguage.C_HEADER) {
            appendLine("#include <TargetConditionals.h>")
            appendLine()
        }
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
