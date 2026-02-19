/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroup
import kotlin.test.*

class ToolingDiagnosticGroupEqualTest {

    @Test
    fun `kgp groups with different categories are not equal`() {
        val defaultGroup: DiagnosticGroup = DiagnosticGroup.Kgp.Default
        val misconfigurationGroup: DiagnosticGroup = DiagnosticGroup.Kgp.Misconfiguration
        val deprecationGroup: DiagnosticGroup = DiagnosticGroup.Kgp.Deprecation
        val experimentalGroup: DiagnosticGroup = DiagnosticGroup.Kgp.Experimental

        assert(defaultGroup != misconfigurationGroup) { "Default group should not be equal to Misconfiguration group" }
        assert(defaultGroup != deprecationGroup) { "Default group should not be equal to Deprecation group" }
        assert(defaultGroup != experimentalGroup) { "Default group should not be equal to Experimental group" }

        assert(misconfigurationGroup != deprecationGroup) { "Misconfiguration group should not be equal to Deprecation group" }
        assert(misconfigurationGroup != experimentalGroup) { "Misconfiguration group should not be equal to Experimental group" }

        assert(deprecationGroup != experimentalGroup) { "Deprecation group should not be equal to Experimental group" }
    }

    @Test
    fun `kgp groups should be equal to themselves`() {
        val groups: List<Pair<DiagnosticGroup, DiagnosticGroup>> = listOf(
            DiagnosticGroup.Kgp.Default to DiagnosticGroup.Kgp.Default,
            DiagnosticGroup.Kgp.Misconfiguration to DiagnosticGroup.Kgp.Misconfiguration,
            DiagnosticGroup.Kgp.Deprecation to DiagnosticGroup.Kgp.Deprecation,
            DiagnosticGroup.Kgp.Experimental to DiagnosticGroup.Kgp.Experimental
        )

        groups.forEach { (group1, group2) ->
            assert(group1 == group2) { "${group1.groupPath} group should be equal to itself" }
        }
    }

    @Test
    fun `kgp groups should be consistent with hashCode`() {
        val groups: List<Pair<DiagnosticGroup, DiagnosticGroup>> = listOf(
            DiagnosticGroup.Kgp.Default to DiagnosticGroup.Kgp.Default,
            DiagnosticGroup.Kgp.Misconfiguration to DiagnosticGroup.Kgp.Misconfiguration,
            DiagnosticGroup.Kgp.Deprecation to DiagnosticGroup.Kgp.Deprecation,
            DiagnosticGroup.Kgp.Experimental to DiagnosticGroup.Kgp.Experimental
        )

        val distinctGroups: List<Pair<DiagnosticGroup, DiagnosticGroup>> = listOf(
            DiagnosticGroup.Kgp.Default to DiagnosticGroup.Kgp.Misconfiguration,
            DiagnosticGroup.Kgp.Default to DiagnosticGroup.Kgp.Deprecation,
            DiagnosticGroup.Kgp.Default to DiagnosticGroup.Kgp.Experimental,
            DiagnosticGroup.Kgp.Misconfiguration to DiagnosticGroup.Kgp.Deprecation,
            DiagnosticGroup.Kgp.Misconfiguration to DiagnosticGroup.Kgp.Experimental,
            DiagnosticGroup.Kgp.Deprecation to DiagnosticGroup.Kgp.Experimental
        )

        // Same groups should have same hashCode
        groups.forEach { (group1, group2) ->
            assert(group1.hashCode() == group2.hashCode()) {
                "${group1.groupPath} group should have same hashCode with itself"
            }
        }

        // Different groups should have different hashCodes
        distinctGroups.forEach { (group1, group2) ->
            assert(group1.hashCode() != group2.hashCode()) {
                "${group1.groupPath} and ${group2.groupPath} groups should have different hashCodes"
            }
        }
    }
}