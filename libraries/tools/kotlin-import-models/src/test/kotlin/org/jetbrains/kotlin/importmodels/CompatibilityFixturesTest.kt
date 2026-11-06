/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels

import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.Result
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Fixtures serialized by Google Protobuf 4.28.2 from an earlier schema revision, which the current classes must keep reading.
 * Unknown field 127 and unknown enum value 101 are deliberate.
 */
class CompatibilityFixturesTest {
    @Test
    fun `reads a project result`() {
        val projectResult = Result.parseFrom(fixture("project-result.bin"))
        assertEquals("type.googleapis.com/org.jetbrains.kotlin.importmodels.proto.ProjectModel", projectResult.model.typeUrl)
        assertEquals(
            listOf(":|:|jvm|main"),
            projectResult.model.unpack(ProjectModel::class.java).compilationUnitIdsList.map { it.value },
        )
    }

    @Test
    fun `reads errors and keeps unknown enum values and fields`() {
        val errorResult = Result.parseFrom(fixture("error-result.bin"))
        assertEquals(Result.ResultCase.ERROR, errorResult.resultCase)
        assertEquals(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS, errorResult.error.errorType)
        assertEquals("fixture error", errorResult.error.errorMessage)

        val unknownEnumError = Error.parseFrom(fixture("unknown-enum-error.bin"))
        assertEquals(101, unknownEnumError.errorTypeValue)

        val unknownFieldError = Error.parseFrom(fixture("unknown-field-error.bin"))
        assertEquals("unknown field", unknownFieldError.errorMessage)
        assertContentEquals(byteArrayOf(0xf8.toByte(), 0x07, 0x01), unknownFieldError.toByteArray().takeLast(3).toByteArray())
    }

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/compatibility/google-protobuf/$name")) { "Missing fixture: $name" }.readBytes()
}
