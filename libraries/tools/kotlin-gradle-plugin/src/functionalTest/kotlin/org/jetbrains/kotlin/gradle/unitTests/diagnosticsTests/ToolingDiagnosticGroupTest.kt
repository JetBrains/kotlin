/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroup
import kotlin.test.*

class ToolingDiagnosticGroupTest {

    @Test
    fun `kgp groups should have correct group paths`() {
        val expectedPaths: Map<DiagnosticGroup, String> = mapOf(
            DiagnosticGroup.Kgp.Default to "kotlin:kgp",
            DiagnosticGroup.Kgp.Deprecation to "kotlin:kgp:deprecation",
            DiagnosticGroup.Kgp.Misconfiguration to "kotlin:kgp:misconfiguration",
            DiagnosticGroup.Kgp.Experimental to "kotlin:kgp:experimental"
        )

        expectedPaths.forEach { (group, expectedPath) ->
            assert(group.groupPath == expectedPath) {
                "Group ${group.groupPath} should have path $expectedPath"
            }
        }
    }

    @Test
    fun `kgp groups should have correct display names`() {
        val expectedDisplayNames: Map<DiagnosticGroup, String> = mapOf(
            DiagnosticGroup.Kgp.Default to "Kotlin Gradle Plugin",
            DiagnosticGroup.Kgp.Deprecation to "Kotlin Gradle Plugin Deprecation",
            DiagnosticGroup.Kgp.Misconfiguration to "Kotlin Gradle Plugin Misconfiguration",
            DiagnosticGroup.Kgp.Experimental to "Kotlin Gradle Plugin Experimental Feature"
        )

        expectedDisplayNames.forEach { (group, expectedDisplayName) ->
            assert(group.displayName == expectedDisplayName) {
                "Group ${group.groupPath} should have display name $expectedDisplayName"
            }
        }
    }

    @Test
    fun `kgp groups should have parent references`() {
        val expectedParents: Map<DiagnosticGroup, DiagnosticGroup> = mapOf(
            DiagnosticGroup.Kgp.Default to DiagnosticGroup.KotlinDiagnosticGroup,
            DiagnosticGroup.Kgp.Deprecation to DiagnosticGroup.KotlinDiagnosticGroup,
            DiagnosticGroup.Kgp.Misconfiguration to DiagnosticGroup.KotlinDiagnosticGroup,
            DiagnosticGroup.Kgp.Experimental to DiagnosticGroup.KotlinDiagnosticGroup
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
            DiagnosticGroup.Kgp.Default.groupId,
            DiagnosticGroup.Kgp.Deprecation.groupId,
            DiagnosticGroup.Kgp.Misconfiguration.groupId,
            DiagnosticGroup.Kgp.Experimental.groupId
        )

        assert(groupIds.toSet().size == groupIds.size) {
            "All KGP diagnostic groups should have unique group IDs"
        }
    }
}