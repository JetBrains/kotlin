/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.importmodels.internal

import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.Result
import org.jetbrains.kotlin.importmodels.proto.baseModel
import org.jetbrains.kotlin.importmodels.proto.version
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinImportModelSerializationTest {
    @Test
    fun `packs a model into a Result`() {
        val bytes = KotlinImportModelSerialization.modelResult(
            baseModel {
                id = KotlinImportModelIds.BASE
                pluginVersion = version {
                    major = 2
                    minor = 5
                    patch = 20
                    classifier = "dev"
                }
                capabilities += BaseModel.Capability.CAPABILITY_KOTLIN_JVM
            }
        )

        val unpacked = Result.parseFrom(bytes).model.unpack(BaseModel::class.java)
        assertEquals(KotlinImportModelIds.BASE, unpacked.id)
        assertEquals(2, unpacked.pluginVersion.major)
        assertEquals(5, unpacked.pluginVersion.minor)
        assertEquals(20, unpacked.pluginVersion.patch)
        assertEquals("dev", unpacked.pluginVersion.classifier)
        assertEquals(listOf(BaseModel.Capability.CAPABILITY_KOTLIN_JVM), unpacked.capabilitiesList)
    }

    @Test
    fun `packs an error into a Result`() {
        val bytes = KotlinImportModelSerialization.errorResult(
            type = Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID,
            message = "Kotlin import model 'unknown' is not known to the producer",
        )

        val error = Result.parseFrom(bytes).error
        assertEquals(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID, error.errorType)
        assertEquals("Kotlin import model 'unknown' is not known to the producer", error.errorMessage)
    }
}
