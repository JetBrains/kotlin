/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

/**
 * Represents a group for diagnostics messages.
 * A diagnostic group helps organize and categorize diagnostics hierarchically.
 *
 * @property groupId A unique identifier for this diagnostic group.
 * @property displayName A human-readable name for this diagnostic group.
 * @property parent The parent group of this diagnostic group, or null if this is a top-level group.
 */
interface DiagnosticGroup {
    val groupId: String
    val displayName: String
    val parent: DiagnosticGroup?
}

/**
 * Provides access to predefined diagnostic groups for the Kotlin Gradle Plugin (KGP).
 *
 * The `DiagnosticGroups` object organizes diagnostic groups under categories such as `kotlin`, `compose`,
 * and subcategories within the `KGP` object to facilitate granular management of diagnostics.
 *
 * These diagnostic groups are used to categorize and structure diagnostic messages hierarchically
 * throughout the Kotlin Gradle Plugin.
 */
internal object DiagnosticGroups {
    val Kotlin: DiagnosticGroup get() = ToolingDiagnosticGroup.KotlinDiagnosticGroup
    val Compose: DiagnosticGroup get() = ToolingDiagnosticGroup.ComposeDiagnosticGroup

    object KGP {
        val Default: DiagnosticGroup get() = ToolingDiagnosticGroup.KGPDiagnosticGroup.Default
        val Deprecation: DiagnosticGroup get() = ToolingDiagnosticGroup.KGPDiagnosticGroup.Deprecation
        val Misconfiguration: DiagnosticGroup get() = ToolingDiagnosticGroup.KGPDiagnosticGroup.Misconfiguration
        val Experimental: DiagnosticGroup get() = ToolingDiagnosticGroup.KGPDiagnosticGroup.Experimental
    }
}

/**
 * Holds constants representing group identifiers for diagnostic messages within the Kotlin Gradle Plugin.
 *
 * These group identifiers are used to classify and organize diagnostics,
 * ensuring they are correctly grouped and identifiable by their categories.
 */
private object GroupId {
    const val KOTLIN = "KOTLIN"
    const val KGP = "KGP"
    const val COMPOSE = "COMPOSE"
}

/**
 * Represents a hierarchical structure of diagnostic groups related to tooling in the Kotlin Gradle Plugin.
 * This sealed class provides predefined diagnostic groups and allows extension for new diagnostic subgroups.
 *
 * @property groupId A unique identifier for the diagnostic group.
 * @property parent The parent diagnostic group, if any.
 */
private sealed class ToolingDiagnosticGroup private constructor(
    override val groupId: String,
    override val parent: DiagnosticGroup? = null
) : DiagnosticGroup {

    abstract override val displayName: String

    override fun toString() = "$groupId | $displayName | parent: [$parent]"

    object KotlinDiagnosticGroup : ToolingDiagnosticGroup(GroupId.KOTLIN) {
        override val displayName: String = "Kotlin"
    }

    class KGPDiagnosticGroup private constructor(
        private val category: String? = null
    ) : ToolingDiagnosticGroup(GroupId.KGP, KotlinDiagnosticGroup) {

        private object Category {
            const val DEPRECATION = "DEPRECATION"
            const val MISCONFIGURATION = "MISCONFIGURATION"
            const val EXPERIMENTAL = "EXPERIMENTAL"

            fun getDisplayName(category: String): String = when (category) {
                DEPRECATION -> "Deprecation"
                MISCONFIGURATION -> "Misconfiguration"
                EXPERIMENTAL -> "Experimental Feature"
                else -> throw IllegalArgumentException("Unknown category: $category")
            }
        }

        override val groupId: String
            get() = buildString {
                append(super.groupId)
                category?.let { append("_$it") }
            }

        override val displayName: String
            get() = buildString {
                append("Kotlin Gradle Plugin")
                category?.let {
                    append(" ${Category.getDisplayName(it)}")
                }
            }

        companion object {
            val Default = KGPDiagnosticGroup()
            val Deprecation = KGPDiagnosticGroup(Category.DEPRECATION)
            val Misconfiguration = KGPDiagnosticGroup(Category.MISCONFIGURATION)
            val Experimental = KGPDiagnosticGroup(Category.EXPERIMENTAL)
        }
    }

    object ComposeDiagnosticGroup : ToolingDiagnosticGroup(GroupId.COMPOSE, KGPDiagnosticGroup.Default) {
        override val displayName: String = "Compose Plugin"
    }
}