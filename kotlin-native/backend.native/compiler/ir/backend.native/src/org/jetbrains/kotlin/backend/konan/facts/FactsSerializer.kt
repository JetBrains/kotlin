/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Path
import kotlin.collections.iterator
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

interface FactsSerializer {
    fun serialize(defs: DefsAssociation, uses: UsesAssociation): ByteArray
}

interface FactsDeserializer {
    fun deserialize(buffer: ByteArray): Pair<DefsAssociation, UsesAssociation>
}

/**
 * CSV with a header row: `relation,path,kind,symbolKey,hash,representation` (hex for the hashes).
 * Fields containing `,`, `"` or line breaks are quoted, e.g. a signature with several type arguments.
 * The representation (e.g., the rendered signature) is only for humans and is not part of the key.
 */
object DebugFactsSerializer : FactsSerializer {
    internal val HEADER = listOf("relation", "path", "kind", "symbolKey", "hash", "representation")

    private fun StringBuilder.appendCsvRow(fields: List<String>) {
        fields.joinTo(this, ",") { it.toCsvField() }
        append('\n')
    }

    private fun StringBuilder.appendFacts(relation: String, association: Map<FileName, Map<FactKey, FactHash>>) {
        for ([path, facts] in association) {
            for ([key, hash] in facts) {
                appendCsvRow(
                        listOf(
                                relation,
                                path,
                                key.kind.name,
                                key.key.signatureHash.toString(16),
                                hash.toString(16),
                                key.representation.orEmpty(),
                        )
                )
            }
        }
    }

    override fun serialize(defs: DefsAssociation, uses: UsesAssociation): ByteArray {
        val text = buildString {
            appendCsvRow(HEADER)
            appendFacts("defines", defs)
            appendFacts("uses", uses)
        }
        return text.toByteArray(Charsets.UTF_8)
    }
}

/**
 * Reads back the output of [DebugFactsSerializer].
 */
object DebugFactsDeserializer : FactsDeserializer {
    override fun deserialize(buffer: ByteArray): Pair<DefsAssociation, UsesAssociation> {
        val defs = mutableMapOf<FileName, MutableMap<FactKey, FactHash>>()
        val uses = mutableMapOf<FileName, MutableMap<FactKey, FactHash>>()
        val rows = parseCsvFrom(buffer.toString(Charsets.UTF_8))
        if (rows.isEmpty()) return defs to uses
        require(rows.first() == DebugFactsSerializer.HEADER) { "Unexpected facts header: ${rows.first()}" }
        for (row in rows.drop(1)) {
            require(row.size == DebugFactsSerializer.HEADER.size) { "Malformed facts row: $row" }
            val association = when (val relation = row[0]) {
                "defines" -> defs
                "uses" -> uses
                else -> error("Unknown relation '$relation' in facts row: $row")
            }
            val key = FactKey(FactKind.valueOf(row[2]), SymbolKey(row[3].toULong(16)), row[5].ifEmpty { null })
            association.getOrPut(row[1]) { mutableMapOf() }[key] = row[4].toULong(16)
        }
        return defs to uses
    }
}

private fun String.toCsvField(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + replace("\"", "\"\"") + "\"" else this

internal fun parseCsvFrom(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var quoted = false

    fun endField() {
        row.add(field.toString())
        field.clear()
        quoted = false
    }

    fun endRow() {
        endField()
        if (row.size > 1 || row.single().isNotEmpty()) rows.add(row)
        row = mutableListOf()
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> {
                field.append('"')
                i++
            }
            inQuotes && c == '"' -> inQuotes = false
            inQuotes -> field.append(c)
            c == '"' && field.isEmpty() && !quoted -> {
                inQuotes = true
                quoted = true
            }
            c == ',' -> endField()
            c == '\n' -> endRow()
            c == '\r' -> {}
            else -> field.append(c)
        }
        i++
    }
    require(!inQuotes) { "Unterminated quoted CSV field" }
    if (field.isNotEmpty() || row.isNotEmpty() || quoted) endRow()
    return rows
}

/**
 * Compact format: `MAGIC VERSION defs uses`, where each association is
 * `fileCount { path entryCount { kindId symbolKey hash } }`.
 * The representation of the keys is not stored, see [DebugFactsSerializer] for a human-readable version.
 */
object BinaryFactsSerializer : FactsSerializer {
    internal const val MAGIC = 0x4B464354 // "KFCT"
    internal const val VERSION = 1

    private fun DataOutputStream.writeAssociation(association: Map<FileName, Map<FactKey, FactHash>>) {
        writeInt(association.size)
        for ([path, facts] in association) {
            val pathBytes = path.toByteArray(Charsets.UTF_8)
            writeInt(pathBytes.size)
            write(pathBytes)
            writeInt(facts.size)
            for ([key, hash] in facts) {
                writeInt(key.kind.id)
                writeLong(key.key.signatureHash.toLong())
                writeLong(hash.toLong())
            }
        }
    }

    override fun serialize(defs: DefsAssociation, uses: UsesAssociation): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use {
            it.writeInt(MAGIC)
            it.writeInt(VERSION)
            it.writeAssociation(defs)
            it.writeAssociation(uses)
        }
        return bytes.toByteArray()
    }
}

object BinaryFactsDeserializer : FactsDeserializer {
    private val kindsById = FactKind.entries.associateBy { it.id }

    private fun DataInputStream.readAssociation(): Map<FileName, Map<FactKey, FactHash>> =
            (0 until readInt()).associate { _ ->
                val path = ByteArray(readInt()).also { readFully(it) }.toString(Charsets.UTF_8)
                val facts = (0 until readInt()).associate { _ ->
                    val kindId = readInt()
                    val kind = kindsById[kindId] ?: error("Unknown fact kind id: $kindId")
                    val symbolKey = SymbolKey(readLong().toULong())
                    val hash = readLong().toULong()
                    FactKey(kind, symbolKey) to hash
                }
                path to facts
            }

    override fun deserialize(buffer: ByteArray): Pair<DefsAssociation, UsesAssociation> =
            DataInputStream(buffer.inputStream()).use {
                val magic = it.readInt()
                require(magic == BinaryFactsSerializer.MAGIC) { "Not a facts file (magic: ${magic.toString(16)})" }
                val version = it.readInt()
                require(version == BinaryFactsSerializer.VERSION) { "Unsupported facts file version: $version" }
                val defs = it.readAssociation()
                val uses = it.readAssociation()
                defs to uses
            }
}

internal fun FactsSerializer.serializeTo(path: Path, defs: DefsAssociation, uses: UsesAssociation) {
    path.writeBytes(serialize(defs, uses))
}

internal fun FactsDeserializer.deserializeFrom(path: Path): Pair<DefsAssociation, UsesAssociation> =
        deserialize(path.readBytes())