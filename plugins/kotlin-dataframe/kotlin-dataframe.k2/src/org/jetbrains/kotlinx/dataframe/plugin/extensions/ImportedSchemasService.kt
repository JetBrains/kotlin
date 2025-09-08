/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.plugin.ImportedSchemaMetadata
import org.jetbrains.kotlinx.dataframe.plugin.impl.*
import java.io.File

class ImportedSchemasService(
    session: FirSession,
    contextReader: ImportedSchemasData.Reader
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(contextReader: ImportedSchemasData.Reader): Factory {
            return Factory { ImportedSchemasService(it, contextReader) }
        }
    }

    val importedSchemasData = contextReader.read()

    val topLevelSchemas: Map<Name, ImportedDataSchema> = importedSchemasData.topLevelCache
    val nestedSchemas: Map<Name, Map<Name, List<SimpleCol>>> = importedSchemasData.nestedCache
    val topLevelMetadata: Map<Name, ImportedSchemaMetadata> = importedSchemasData.topLevelMetadata
}

val FirSession.importedSchemasService: ImportedSchemasService by FirSession.sessionComponentAccessor()

class ImportedSchemasData(
    val topLevelCache: Map<Name, ImportedDataSchema>,
    val nestedCache: Map<Name, Map<Name, List<SimpleCol>>>,
    val topLevelMetadata: Map<Name, ImportedSchemaMetadata>,
    val errors: Map<String, ParseResult.Failure>
) {
    companion object {
        fun getReader(path: String?): Reader? {
            return path?.let { File(path) }?.takeIf { it.exists() && it.isDirectory }?.let { directory ->
                Reader {
                    val parser = PluginDataFrameSchemaParser()
                    val schemas: Map<String, ParseResult> = directory.walkTopDown()
                        .filter { it.isFile && it.extension == "json" }
                        .associate { it.nameWithoutExtension to parser.parseSchemaWithMeta(it.readText()) }
                    createDataContext(schemas)
                }
            }
        }

        fun getReader(inMemory: Map<String, String>): Reader {
            val parser = PluginDataFrameSchemaParser()
            return Reader { createDataContext(inMemory.mapValues { parser.parseSchemaWithMeta(it.value) }) }
        }
    }

    fun interface Reader {
        fun read(): ImportedSchemasData
    }
}

fun createDataContext(schemas: Map<String, ParseResult>): ImportedSchemasData {
    val errors = schemas.mapNotNull { entry -> (entry.value as? ParseResult.Failure)?.let { entry.key to it } }.toMap()
    val schemas = schemas.mapNotNull { entry -> (entry.value as? ImportedDataSchema)?.let { entry.key to it } }.toMap()
    val topLevelCache: Map<Name, ImportedDataSchema> = schemas.map { Name.identifier(it.key) to it.value }.toMap()
    val nestedCache: Map<Name, Map<Name, List<SimpleCol>>> = gatherSchemas(schemas)
    val topLevelMetadata = topLevelCache.mapValues { it.value.metadata }
    return ImportedSchemasData(topLevelCache, nestedCache, topLevelMetadata, errors)
}

fun gatherSchemas(schemas: Map<String, ImportedDataSchema>): Map<Name, MutableMap<Name, List<SimpleCol>>> = buildMap {
    fun gatherImpl(name: Name, columns: List<SimpleCol>) {
        columns.forEach { column ->
            val childColumns = when (column) {
                is SimpleColumnGroup -> column.columns()
                is SimpleFrameColumn -> column.columns()
                is SimpleDataColumn -> return@forEach
            }
            getOrPut(name) { mutableMapOf() }[Name.identifier(column.name)] = childColumns
            gatherImpl(name, childColumns)
        }
    }

    schemas.forEach { (name, schema) ->
        gatherImpl(Name.identifier(name), schema.schema.columns())
    }
}