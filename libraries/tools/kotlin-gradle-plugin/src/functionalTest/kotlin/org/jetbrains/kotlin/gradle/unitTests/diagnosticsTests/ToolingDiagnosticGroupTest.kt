/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroups
import kotlin.test.*

class ToolingDiagnosticGroupTest {

    @Test
    fun `kgp groups should have correct group paths`() {
        val expectedPaths = mapOf(
            DiagnosticGroups.KGP.Default to "kotlin:kgp",
            DiagnosticGroups.KGP.Deprecation to "kotlin:kgp:deprecation",
            DiagnosticGroups.KGP.Misconfiguration to "kotlin:kgp:misconfiguration",
            DiagnosticGroups.KGP.Experimental to "kotlin:kgp:experimental"
        )

        expectedPaths.forEach { (group, expectedPath) ->
            assert(group.groupPath == expectedPath) {
                "Group ${group.groupPath} should have path $expectedPath"
            }
        }
    }

    @Test
    fun `kgp groups should have correct display names`() {
        val expectedDisplayNames = mapOf(
            DiagnosticGroups.KGP.Default to "Kotlin Gradle Plugin",
            DiagnosticGroups.KGP.Deprecation to "Kotlin Gradle Plugin Deprecation",
            DiagnosticGroups.KGP.Misconfiguration to "Kotlin Gradle Plugin Misconfiguration",
            DiagnosticGroups.KGP.Experimental to "Kotlin Gradle Plugin Experimental Feature"
        )

        expectedDisplayNames.forEach { (group, expectedDisplayName) ->
            assert(group.displayName == expectedDisplayName) {
                "Group ${group.groupPath} should have display name $expectedDisplayName"
            }
        }
    }

    @Test
    fun `kgp groups should have parent references`() {
        val expectedParents = mapOf(
            DiagnosticGroups.KGP.Default to DiagnosticGroups.Kotlin,
            DiagnosticGroups.KGP.Deprecation to DiagnosticGroups.Kotlin,
            DiagnosticGroups.KGP.Misconfiguration to DiagnosticGroups.Kotlin,
            DiagnosticGroups.KGP.Experimental to DiagnosticGroups.Kotlin
        )

        expectedParents.forEach { (group, expectedParent) ->
            assert(group.parent == expectedParent) {
                "Group ${group.groupPath} should have parent ${expectedParent.groupPath}"
            }
        }
    }

    @Test
    fun `kgp groups should have unique group ids`() {
        val groupIds = listOf(
            DiagnosticGroups.KGP.Default.groupId,
            DiagnosticGroups.KGP.Deprecation.groupId,
            DiagnosticGroups.KGP.Misconfiguration.groupId,
            DiagnosticGroups.KGP.Experimental.groupId
        )

        assert(groupIds.toSet().size == groupIds.size) {
            "All KGP diagnostic groups should have unique group IDs"
        }
    }
}