/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import java.util.*

/**
 * Represents a grouping mechanism for diagnostics, which helps categorize diagnostics
 * based on logical or hierarchical groupings.
 *
 * @property groupId A unique identifier for the diagnostic group.
 * @property displayName A user-friendly name of the diagnostic group.
 * @property groupPath The fully qualified path of the diagnostic group, which may reflect
 * hierarchical nesting of groups.
 * @property parent The parent diagnostic group of this group, or null if this group has
 * no parent and is at the top level.
 *
 * @since 2.2.0
 */
internal sealed interface DiagnosticGroup {
    val groupId: String
    val displayName: String
    val groupPath: String
    val parent: DiagnosticGroup?

    /**
     * Base implementation for DiagnosticGroup that provides common functionality.
     */
    abstract class Base : DiagnosticGroup {
        override fun toString() = "$groupId | $displayName | parent: [$parent]"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DiagnosticGroup) return false
            return groupId == other.groupId && parent == other.parent
        }

        override fun hashCode(): Int = Objects.hash(groupId, parent)
    }

    /**
     * Represents a diagnostic group for generic Kotlin related diagnostics.
     */
    object KotlinDiagnosticGroup : Base() {
        override val groupId: String = GroupId.KOTLIN
        override val displayName: String = "Kotlin"
        override val parent: DiagnosticGroup? = null
        override val groupPath: String = groupId.lowercase(Locale.getDefault())
    }

    /**
     * Represents a hierarchical structure of diagnostic groups related to KGP.
     */
    sealed class Kgp(
        private val category: String? = null
    ) : Base() {
        override val groupId: String = when (category) {
            Category.DEPRECATION, Category.MISCONFIGURATION, Category.EXPERIMENTAL -> "${GroupId.KGP}:$category"
            else -> GroupId.KGP
        }

        override val parent: DiagnosticGroup = KotlinDiagnosticGroup

        override val displayName: String
            get() = buildString {
                append("Kotlin Gradle Plugin")
                category?.let {
                    append(" ${Category.getDisplayName(it)}")
                }
            }

        override val groupPath: String = buildString {
            append(parent.groupPath)
            append(":")
            append(groupId.lowercase(Locale.getDefault()))
        }

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

        object Default : Kgp()
        object Deprecation : Kgp(Category.DEPRECATION)
        object Misconfiguration : Kgp(Category.MISCONFIGURATION)
        object Experimental : Kgp(Category.EXPERIMENTAL)
    }
}

/**
 * Holds constants representing group identifiers for diagnostic messages within the Kotlin Gradle Plugin.
 *
 * These group identifiers are used to classify and organize diagnostics,
 * ensuring they are correctly grouped and identifiable by their categories.
 *
 * @since 2.2.0
 */
private object GroupId {
    const val KOTLIN = "KOTLIN"
    const val KGP = "KGP"
}