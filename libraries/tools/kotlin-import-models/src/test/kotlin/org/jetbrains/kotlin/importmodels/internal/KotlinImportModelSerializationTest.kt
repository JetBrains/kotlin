/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels.internal

import org.jetbrains.kotlin.importmodels.proto.DependenciesModelKt
import org.jetbrains.kotlin.importmodels.proto.DependenciesModel
import org.jetbrains.kotlin.importmodels.proto.compilationUnitId
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinImportModelSerializationTest {
    @Test
    fun `parses dependency parameters with compilation scope and coverage`() {
        val compilationId = compilationUnitId { value = ":|:|jvm|main" }
        val parameters = DependenciesModelKt.parameters {
            compilationUnitId = compilationId
            scope = DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE
            coverage = DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL
        }.toByteArray()

        val parsed = KotlinImportModelSerialization.parseDependenciesParameters(parameters)

        assertEquals(compilationId, parsed?.compilationUnitId)
        assertEquals(DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE, parsed?.scope)
        assertEquals(DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL, parsed?.coverage)
    }
}
