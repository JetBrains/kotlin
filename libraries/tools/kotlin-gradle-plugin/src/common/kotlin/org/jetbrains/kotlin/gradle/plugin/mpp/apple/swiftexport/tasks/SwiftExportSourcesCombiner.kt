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
 * conditional compilation branch for the destination of that target.
 */

internal enum class SwiftPackageSourceLanguage {
    SWIFT, C_HEADER
}

private const val UNSUPPORTED_DESTINATION = "This Swift package exported from Kotlin does not support the current destination"

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
 * and content that is the same for every target is returned as it is.
 *
 * A branch has the whole file with its imports, because an import can be platform specific as well.
 */
internal fun combineSwiftExportSources(
    variants: List<SwiftExportSourceVariant>,
    language: SwiftPackageSourceLanguage,
): String {
    require(variants.isNotEmpty()) { "Nothing to combine" }

    val targetsByContent = LinkedHashMap<String, MutableList<KonanTarget>>()
    variants.forEach { variant -> targetsByContent.getOrPut(variant.content) { mutableListOf() }.add(variant.target) }
    if (targetsByContent.size == 1) return variants.first().content

    val (firstBranch, nextBranch, unsupported) = when (language) {
        SwiftPackageSourceLanguage.SWIFT -> Triple("#if", "#elseif", "#error(\"$UNSUPPORTED_DESTINATION\")")
        SwiftPackageSourceLanguage.C_HEADER -> Triple("#if", "#elif", "#error \"$UNSUPPORTED_DESTINATION\"")
    }

    return buildString {
        if (language == SwiftPackageSourceLanguage.C_HEADER) {
            appendLine("#include <TargetConditionals.h>")
            appendLine()
        }
        targetsByContent.entries.forEachIndexed { index, (content, targets) ->
            val conditions = targets.map { it.swiftPackageDestinationCondition(language) }
            val condition = conditions.singleOrNull() ?: conditions.joinToString(" || ") { "($it)" }
            appendLine("${if (index == 0) firstBranch else nextBranch} $condition")
            appendLine(content.trimEnd('\n'))
        }
        appendLine("#else")
        appendLine(unsupported)
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
 * What has to be the same for every target. Combining targets with different Swift modules is not supported.
 */
private data class SwiftModuleShape(val isBridged: Boolean, val bridgeName: String?, val dependencies: List<String>)

private fun SwiftExportTargetModules.shapes(): Map<String, SwiftModuleShape> = modules.associate { module ->
    module.name to SwiftModuleShape(
        isBridged = module is GradleSwiftExportModule.BridgesToKotlin,
        bridgeName = (module as? GradleSwiftExportModule.BridgesToKotlin)?.bridgeName,
        dependencies = module.dependencies.sorted(),
    )
}

/**
 * Two targets with different Swift modules, by their names in the build script.
 */
internal data class SwiftExportModuleGraphMismatch(
    val referenceTarget: String,
    val otherTarget: String,
    val differences: List<String>,
)

/**
 * Compares the modules of [targets] with the modules of the first one. Returns the first difference found.
 */
internal fun findSwiftExportModuleGraphMismatch(targets: List<SwiftExportTargetModules>): SwiftExportModuleGraphMismatch? {
    val reference = targets.firstOrNull() ?: return null
    val referenceShapes = reference.shapes()

    for (other in targets.drop(1)) {
        val otherShapes = other.shapes()
        if (otherShapes == referenceShapes) continue

        val differences = (referenceShapes.keys + otherShapes.keys).sorted().mapNotNull { name ->
            val expected = referenceShapes[name]
            val actual = otherShapes[name]
            when {
                expected == actual -> null
                actual == null -> "$name: only for ${reference.targetName}"
                expected == null -> "$name: only for ${other.targetName}"
                expected.dependencies != actual.dependencies ->
                    "$name: depends on ${expected.dependencies} for ${reference.targetName} " +
                            "and on ${actual.dependencies} for ${other.targetName}"
                else -> "$name: bridges to Kotlin differently for ${reference.targetName} and ${other.targetName}"
            }
        }

        return SwiftExportModuleGraphMismatch(reference.targetName, other.targetName, differences)
    }
    return null
}
