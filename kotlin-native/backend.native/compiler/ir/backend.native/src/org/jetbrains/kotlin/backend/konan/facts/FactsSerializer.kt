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

fun interface FactsSerializer {
    fun serialize(defs: DefsAssociation, uses: UsesAssociation): ByteArray
}

fun interface FactsDeserializer {
    fun deserialize(buffer: ByteArray): Pair<DefsAssociation, UsesAssociation>
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