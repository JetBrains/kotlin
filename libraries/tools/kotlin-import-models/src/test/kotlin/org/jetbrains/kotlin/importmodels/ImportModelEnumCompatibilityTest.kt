/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels

import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.DependenciesModel
import org.jetbrains.kotlin.importmodels.proto.Error
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportModelEnumCompatibilityTest {
    @Test
    fun `compilation purpose and output kind match the RFC contract`() {
        val compilation = CompilationUnitModel.getDescriptor()
        assertEquals("purpose", compilation.findFieldByNumber(5).name)
        assertEquals(
            listOf(
                "COMPILATION_PURPOSE_UNSPECIFIED",
                "COMPILATION_PURPOSE_MAIN",
                "COMPILATION_PURPOSE_TEST",
                "COMPILATION_PURPOSE_TEST_FIXTURES",
            ),
            compilation.findEnumTypeByName("Purpose").values.map { it.name },
        )

        val output = compilation.findNestedTypeByName("Output")
        assertEquals("kind", output.findFieldByNumber(3).name)
        assertEquals(
            listOf("OUTPUT_KIND_UNSPECIFIED", "OUTPUT_KIND_CLASSES", "OUTPUT_KIND_CRI"),
            output.findEnumTypeByName("Kind").values.map { it.name },
        )
    }

    @Test
    fun `uses unspecified zero defaults`() {
        assertEquals(0, Error.Type.ERROR_TYPE_UNSPECIFIED.number)
        assertEquals(0, BaseModel.Capability.CAPABILITY_UNSPECIFIED.number)
        assertEquals(0, CompilationUnitModel.Platform.PLATFORM_UNSPECIFIED.number)
        assertEquals(0, CompilationUnitModel.Purpose.COMPILATION_PURPOSE_UNSPECIFIED.number)
        assertEquals(0, CompilationUnitModel.Output.Kind.OUTPUT_KIND_UNSPECIFIED.number)
        assertEquals(0, DependenciesModel.SourceDependencyKind.SOURCE_DEPENDENCY_KIND_UNSPECIFIED.number)
    }

    @Test
    fun `preserves and surfaces unrecognized enum values`() {
        val error = Error.parseFrom(Error.newBuilder().setErrorTypeValue(101).build().toByteArray())
        assertEquals(101, error.errorTypeValue)
        assertEquals(Error.Type.UNRECOGNIZED, error.errorType)

        val model = BaseModel.parseFrom(BaseModel.newBuilder().addCapabilitiesValue(102).build().toByteArray())
        assertEquals(listOf(102), model.capabilitiesValueList)
        assertEquals(listOf(BaseModel.Capability.UNRECOGNIZED), model.capabilitiesList)

        val compilation = CompilationUnitModel.parseFrom(
            CompilationUnitModel.newBuilder().setPlatformValue(103).build().toByteArray()
        )
        assertEquals(103, compilation.platformValue)
        assertEquals(CompilationUnitModel.Platform.UNRECOGNIZED, compilation.platform)

        val purpose = CompilationUnitModel.parseFrom(
            CompilationUnitModel.newBuilder().setPurposeValue(105).build().toByteArray()
        )
        assertEquals(105, purpose.purposeValue)
        assertEquals(CompilationUnitModel.Purpose.UNRECOGNIZED, purpose.purpose)

        val output = CompilationUnitModel.Output.parseFrom(
            CompilationUnitModel.Output.newBuilder().setKindValue(106).build().toByteArray()
        )
        assertEquals(106, output.kindValue)
        assertEquals(CompilationUnitModel.Output.Kind.UNRECOGNIZED, output.kind)

        val sourceDependency = DependenciesModel.SourceDependency.parseFrom(
            DependenciesModel.SourceDependency.newBuilder().setKindValue(104).build().toByteArray()
        )
        assertEquals(104, sourceDependency.kindValue)
        assertEquals(DependenciesModel.SourceDependencyKind.UNRECOGNIZED, sourceDependency.kind)
    }
}
