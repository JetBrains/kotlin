/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroup
import kotlin.test.*

class CompilerDiagnosticGroupTest {

    @Test
    fun `compiler groups should have correct group paths`() {
        val expectedPaths: Map<DiagnosticGroup, String> = mapOf(
            DiagnosticGroup.Compiler.Default to "kotlin:compiler",
            DiagnosticGroup.Compiler.Error to "kotlin:compiler:error",
            DiagnosticGroup.Compiler.Warning to "kotlin:compiler:warning",
        )

        expectedPaths.forEach { (group, expectedPath) ->
            assert(group.groupPath == expectedPath) {
                "Group ${group.groupPath} should have path $expectedPath"
            }
        }
    }

    @Test
    fun `compiler groups should have correct display names`() {
        val expectedDisplayNames: Map<DiagnosticGroup, String> = mapOf(
            DiagnosticGroup.Compiler.Default to "Kotlin Compiler",
            DiagnosticGroup.Compiler.Error to "Kotlin Compiler Error",
            DiagnosticGroup.Compiler.Warning to "Kotlin Compiler Warning",
        )

        expectedDisplayNames.forEach { (group, expectedDisplayName) ->
            assert(group.displayName == expectedDisplayName) {
                "Group ${group.groupPath} should have display name $expectedDisplayName"
            }
        }
    }

    @Test
    fun `compiler groups should have correct parent references`() {
        val expectedParents: Map<DiagnosticGroup, DiagnosticGroup> = mapOf(
            DiagnosticGroup.Compiler.Default to DiagnosticGroup.KotlinDiagnosticGroup,
            DiagnosticGroup.Compiler.Error to DiagnosticGroup.KotlinDiagnosticGroup,
            DiagnosticGroup.Compiler.Warning to DiagnosticGroup.KotlinDiagnosticGroup,
        )

        expectedParents.forEach { (group, expectedParent) ->
            assert(group.parent == expectedParent) {
                "Group ${group.groupPath} should have parent ${expectedParent.groupPath}"
            }
        }
    }

    @Test
    fun `compiler groups should have unique group ids`() {
        val groupIds = listOf(
            DiagnosticGroup.Compiler.Default.groupId,
            DiagnosticGroup.Compiler.Error.groupId,
            DiagnosticGroup.Compiler.Warning.groupId,
        )

        assert(groupIds.toSet().size == groupIds.size) {
            "All Compiler diagnostic groups should have unique group IDs"
        }
    }

    @Test
    fun `compiler groups should have correct group ids`() {
        val expectedGroupIds: Map<DiagnosticGroup, String> = mapOf(
            DiagnosticGroup.Compiler.Default to "COMPILER",
            DiagnosticGroup.Compiler.Error to "COMPILER:ERROR",
            DiagnosticGroup.Compiler.Warning to "COMPILER:WARNING",
        )

        expectedGroupIds.forEach { (group, expectedGroupId) ->
            assert(group.groupId == expectedGroupId) {
                "Group ${group.groupPath} should have groupId $expectedGroupId but was ${group.groupId}"
            }
        }
    }
}
