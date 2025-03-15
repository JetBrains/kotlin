/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroups
import kotlin.test.*

class ToolingDiagnosticGroupEqualTest {

    @Test
    fun `kgp groups with different categories are not equal`() {
        val defaultGroup = DiagnosticGroups.KGP.Default
        val misconfigurationGroup = DiagnosticGroups.KGP.Misconfiguration
        val deprecationGroup = DiagnosticGroups.KGP.Deprecation
        val experimentalGroup = DiagnosticGroups.KGP.Experimental

        assert(defaultGroup != misconfigurationGroup) { "Default group should not be equal to Misconfiguration group" }
        assert(defaultGroup != deprecationGroup) { "Default group should not be equal to Deprecation group" }
        assert(defaultGroup != experimentalGroup) { "Default group should not be equal to Experimental group" }

        assert(misconfigurationGroup != deprecationGroup) { "Misconfiguration group should not be equal to Deprecation group" }
        assert(misconfigurationGroup != experimentalGroup) { "Misconfiguration group should not be equal to Experimental group" }

        assert(deprecationGroup != experimentalGroup) { "Deprecation group should not be equal to Experimental group" }
    }

    @Test
    fun `kgp groups should be equal to themselves`() {
        val groups = listOf(
            DiagnosticGroups.KGP.Default to DiagnosticGroups.KGP.Default,
            DiagnosticGroups.KGP.Misconfiguration to DiagnosticGroups.KGP.Misconfiguration,
            DiagnosticGroups.KGP.Deprecation to DiagnosticGroups.KGP.Deprecation,
            DiagnosticGroups.KGP.Experimental to DiagnosticGroups.KGP.Experimental
        )

        groups.forEach { (group1, group2) ->
            assert(group1 == group2) { "${group1.groupPath} group should be equal to itself" }
        }
    }

    @Test
    fun `kgp groups should be consistent with hashCode`() {
        val groups = listOf(
            DiagnosticGroups.KGP.Default to DiagnosticGroups.KGP.Default,
            DiagnosticGroups.KGP.Misconfiguration to DiagnosticGroups.KGP.Misconfiguration,
            DiagnosticGroups.KGP.Deprecation to DiagnosticGroups.KGP.Deprecation,
            DiagnosticGroups.KGP.Experimental to DiagnosticGroups.KGP.Experimental
        )

        val distinctGroups = listOf(
            DiagnosticGroups.KGP.Default to DiagnosticGroups.KGP.Misconfiguration,
            DiagnosticGroups.KGP.Default to DiagnosticGroups.KGP.Deprecation,
            DiagnosticGroups.KGP.Default to DiagnosticGroups.KGP.Experimental,
            DiagnosticGroups.KGP.Misconfiguration to DiagnosticGroups.KGP.Deprecation,
            DiagnosticGroups.KGP.Misconfiguration to DiagnosticGroups.KGP.Experimental,
            DiagnosticGroups.KGP.Deprecation to DiagnosticGroups.KGP.Experimental
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