/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.plugin.diagnostics.DiagnosticGroup
import kotlin.test.*

class CompilerDiagnosticGroupEqualTest {

    @Test
    fun `compiler groups with different categories are not equal`() {
        val defaultGroup: DiagnosticGroup = DiagnosticGroup.Compiler.Default
        val errorGroup: DiagnosticGroup = DiagnosticGroup.Compiler.Error
        val warningGroup: DiagnosticGroup = DiagnosticGroup.Compiler.Warning

        assert(defaultGroup != errorGroup) { "Default group should not be equal to Error group" }
        assert(defaultGroup != warningGroup) { "Default group should not be equal to Warning group" }
        assert(errorGroup != warningGroup) { "Error group should not be equal to Warning group" }
    }

    @Test
    fun `compiler groups should be equal to themselves`() {
        val groups: List<Pair<DiagnosticGroup, DiagnosticGroup>> = listOf(
            DiagnosticGroup.Compiler.Default to DiagnosticGroup.Compiler.Default,
            DiagnosticGroup.Compiler.Error to DiagnosticGroup.Compiler.Error,
            DiagnosticGroup.Compiler.Warning to DiagnosticGroup.Compiler.Warning,
        )

        groups.forEach { (group1, group2) ->
            assert(group1 == group2) { "${group1.groupPath} group should be equal to itself" }
        }
    }

    @Test
    fun `compiler groups should be consistent with hashCode`() {
        val groups: List<Pair<DiagnosticGroup, DiagnosticGroup>> = listOf(
            DiagnosticGroup.Compiler.Default to DiagnosticGroup.Compiler.Default,
            DiagnosticGroup.Compiler.Error to DiagnosticGroup.Compiler.Error,
            DiagnosticGroup.Compiler.Warning to DiagnosticGroup.Compiler.Warning,
        )

        // Equal groups must have equal hashCodes (hashCode contract)
        groups.forEach { (group1, group2) ->
            assert(group1.hashCode() == group2.hashCode()) {
                "${group1.groupPath} group should have same hashCode with itself"
            }
        }
    }
}
