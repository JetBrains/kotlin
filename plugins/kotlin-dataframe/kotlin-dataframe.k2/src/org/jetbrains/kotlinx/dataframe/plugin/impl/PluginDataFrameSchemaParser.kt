/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaMetadata
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.TypeApproximation

sealed class ParseResult {
    object Failure : ParseResult()
}

data class ImportedDataSchema(
    val schema: PluginDataFrameSchema,
    val metadata: ImportedSchemaMetadata
) : ParseResult()

class PluginDataFrameSchemaParser {

    fun parseSchemaWithMeta(@Language("json") jsonString: String): ParseResult {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val jsonElement = json.parseToJsonElement(jsonString)
            parseSchemaWithMeta(jsonElement).getOrElse { ParseResult.Failure }
        } catch (_: Throwable) {
            ParseResult.Failure
        }
    }

    private fun parseSchemaWithMeta(jsonElement: JsonElement): Result<ImportedDataSchema> {
        return when (jsonElement) {
            is JsonObject -> {
                val schemaElement = jsonElement["schema"]
                    ?: return Result.parsingError("Missing 'schema' field in root object")

                parseSchemaObject(schemaElement).flatMap { schema ->
                    val metadata = jsonElement.entries
                        .filterNot { it.key in setOf("schema") }
                        .associate { it.key to it.value }

                    val format = metadata["format"]?.jsonPrimitive?.content
                        ?: return Result.parsingError("Missing 'format' field in metadata")

                    val data = metadata["data"]?.jsonPrimitive?.content
                        ?: return Result.parsingError("Missing 'data' field in metadata")

                    Result.success(
                        ImportedDataSchema(
                            schema,
                            ImportedSchemaMetadata(
                                format = format,
                                data = data
                            ),
                        )
                    )
                }
            }
            else -> Result.parsingError("Expected JsonObject at root level, got ${jsonElement::class.simpleName}")
        }
    }

    fun parseSchemaObject(jsonElement: JsonElement): Result<PluginDataFrameSchema> {
        return when (jsonElement) {
            is JsonObject -> {
                parseColumns(jsonElement).map { columns ->
                    PluginDataFrameSchema(columns)
                }
            }
            else -> Result.parsingError("Expected JsonObject for schema, got ${jsonElement::class.simpleName}")
        }
    }

    private fun parseColumns(jsonObject: JsonObject): Result<List<SimpleCol>> {
        val columns = mutableListOf<SimpleCol>()

        for ((name, value) in jsonObject.entries) {
            if (name.isBlank()) {
                return Result.parsingError("Column name cannot be blank")
            }

            parseColumn(name, value).fold(
                onSuccess = { columns.add(it) },
                onFailure = { return Result.failure(it) }
            )
        }

        return Result.success(columns)
    }

    private fun parseColumn(name: String, value: JsonElement): Result<SimpleCol> {
        return when (value) {
            is JsonPrimitive -> {
                if (value.isString) {
                    val typeString = value.content
                    if (typeString.isBlank()) {
                        Result.parsingError("Type string cannot be blank for column '$name'")
                    } else {
                        createTypeApproximation(typeString).map { typeApproximation ->
                            SimpleDataColumn(name, typeApproximation)
                        }
                    }
                } else {
                    Result.parsingError("Expected string type for column '$name', got ${value.content}")
                }
            }
            is JsonObject -> {
                parseNestedColumns(value).flatMap { nestedColumns ->
                    if (nestedColumns.isEmpty()) {
                        Result.parsingError("'$name' must have at least one nested column")
                    } else {
                        when {
                            name.endsWith(": ColumnGroup") ->Result.success(
                                SimpleColumnGroup(name.removeSuffix(": ColumnGroup"), nestedColumns)
                            )

                            name.endsWith(": FrameColumn") -> Result.success(
                                SimpleFrameColumn(name.removeSuffix(": FrameColumn"), nestedColumns)
                            )

                            else -> Result.parsingError("Column name '$name' needs to end with ': ColumnGroup' or ': FrameColumn'")
                        }
                    }
                }
            }
            else -> Result.parsingError("Unsupported JSON element type for column '$name': ${value::class.simpleName}")
        }
    }

    private fun parseNestedColumns(jsonObject: JsonObject): Result<List<SimpleCol>> {
        val columns = mutableListOf<SimpleCol>()

        for ((key, value) in jsonObject.entries) {
            if (key.isBlank()) {
                return Result.failure(IllegalArgumentException("Nested column name cannot be blank"))
            }

            parseColumn(key, value).fold(
                onSuccess = { columns.add(it) },
                onFailure = { return Result.failure(it) }
            )
        }

        return Result.success(columns)
    }

    private fun createTypeApproximation(typeString: String): Result<TypeApproximation> {
        return try {
            val split = typeString.split(".")

            val lastSegment = split.last()
            val nullable = lastSegment.endsWith("?")
            val typeName = lastSegment.removeSuffix("?")

            val typeApproximation = ClassId(
                FqName(split.dropLast(1).joinToString(".")),
                Name.identifier(typeName)
            ).constructClassLikeType(isMarkedNullable = nullable).wrap()

            Result.success(typeApproximation)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to create type approximation for '$typeString': ${e.message}", e))
        }
    }
}

private inline fun <T, R> Result<T>.flatMap(f: (T) -> Result<R>): Result<R> {
    return fold(
        onSuccess = { f(it) },
        onFailure = { Result.failure(it) }
    )
}

private fun <T> Result.Companion.parsingError(message: String): Result<T> = Result.failure(IllegalArgumentException(message))