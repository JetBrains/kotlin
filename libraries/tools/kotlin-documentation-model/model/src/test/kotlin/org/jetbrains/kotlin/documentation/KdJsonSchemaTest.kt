/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.schema.generator.json.serialization.SerializationClassJsonSchemaGenerator
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.fail

private val overwriteExpectedOutput = System.getProperty("kdm.test.overwrite")?.toBoolean() ?: false // use -Dkdm.test.overwrite=true

class KdJsonSchemaTest {
    private val schemaJson = Json {
        prettyPrint = true
    }

    @Test
    fun `the JSON schema generated for KdBundle matches the checked-in golden file`() {
        val generator = SerializationClassJsonSchemaGenerator(json = schemaJson)
        val actualSchema = generator.generateSchemaString(KdBundle.serializer().descriptor)

        val expectedSchemaFile = File("src/test/resources/kdm-bundle.schema.json")
        val expectedSchema = expectedSchemaFile.readText()

        if (actualSchema.trim() != expectedSchema.trim()) {
            if (overwriteExpectedOutput) {
                expectedSchemaFile.writeText(actualSchema + "\n")
            }
            fail(
                "The JSON schema generated for KdBundle differs from the checked-in golden file " +
                        "'${expectedSchemaFile.name}'. If this is an intentional, reviewed change to the KDM wire " +
                        "format, rerun with -Dkdm.test.overwrite=true to update it" +
                        (if (overwriteExpectedOutput) " (file has been updated, rerun the test to verify)." else ".")
            )
        }
    }
}
