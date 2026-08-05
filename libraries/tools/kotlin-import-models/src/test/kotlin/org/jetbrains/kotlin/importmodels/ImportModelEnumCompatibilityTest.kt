/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels

import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.Capability
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.ErrorType
import org.jetbrains.kotlin.importmodels.proto.Platform
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportModelEnumCompatibilityTest {
    @Test
    fun `uses unspecified zero defaults`() {
        assertEquals(0, ErrorType.ERROR_TYPE_UNSPECIFIED.number)
        assertEquals(0, Capability.CAPABILITY_UNSPECIFIED.number)
        assertEquals(0, Platform.PLATFORM_UNSPECIFIED.number)
    }

    @Test
    fun `preserves and surfaces unrecognized enum values`() {
        val error = Error.parseFrom(Error.newBuilder().setErrorTypeValue(101).build().toByteArray())
        assertEquals(101, error.errorTypeValue)
        assertEquals(ErrorType.UNRECOGNIZED, error.errorType)

        val model = BaseModel.parseFrom(BaseModel.newBuilder().addCapabilitiesValue(102).build().toByteArray())
        assertEquals(listOf(102), model.capabilitiesValueList)
        assertEquals(listOf(Capability.UNRECOGNIZED), model.capabilitiesList)

        val compilation = CompilationUnitModel.parseFrom(
            CompilationUnitModel.newBuilder().setPlatformValue(103).build().toByteArray()
        )
        assertEquals(103, compilation.platformValue)
        assertEquals(Platform.UNRECOGNIZED, compilation.platform)
    }
}
