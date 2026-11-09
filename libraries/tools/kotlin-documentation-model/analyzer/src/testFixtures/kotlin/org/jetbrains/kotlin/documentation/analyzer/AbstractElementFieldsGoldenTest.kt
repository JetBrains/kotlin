/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import org.jetbrains.kotlin.documentation.KdElement
import org.jetbrains.kotlin.documentation.KdJson
import org.jetbrains.kotlin.documentation.KdModule
import org.jetbrains.kotlin.documentation.KdPackage

/**
 * Runs the element-field golden tests in `ElementFieldsGoldenTestGenerated`.
 * Each fixture's filename selects one serialized [KdElement] field to print for all elements in every fragment,
 * including embedded declarations. Entries include a short signature and element ID; absent fields, null values,
 * and empty lists are omitted. Expected output is stored beside the fixture, with variants where Java analysis modes differ.
 */
abstract class AbstractElementFieldsGoldenTest : AbstractKdmGoldenTest() {
    fun runTest(testDataFilePath: String) {
        val file = testDataFile(testDataFilePath)
        val fieldName = file.nameWithoutExtension
        val bundle = generateDocumentationBundleFromTestData(file).canonicalized()
        val json = Json(from = KdJson) { prettyPrint = true }
        val actual = buildString {
            for (fragment in bundle.fragments) {
                appendLine("fragment ${fragment.name}")
                for ((mapName, elements) in listOf("elements" to fragment.elements, "embeddedElements" to fragment.embeddedElements)) {
                    for ((id, element) in elements) {
                        val fields = json.encodeToJsonElement(KdElement.serializer(), element).jsonObject
                        val value = fields[fieldName] ?: continue
                        if (value == JsonNull) continue
                        if (value is JsonArray && value.isEmpty()) continue
                        val signature = element.signatureOrNull() ?: when (element) {
                            is KdModule -> "module ${element.name}"
                            is KdPackage -> "package ${element.name.ifEmpty { "<root>" }}"
                            else -> element.javaClass.simpleName
                        }
                        val prefix = if (mapName == "embeddedElements") "$mapName: " else ""
                        appendLine("$prefix$signature [${id.value}].$fieldName:")
                        appendLine(json.encodeToString(JsonElement.serializer(), value))
                    }
                }
            }
        }
        assertMatchesGoldenTextFile(actual, file, ".txt")
    }
}
