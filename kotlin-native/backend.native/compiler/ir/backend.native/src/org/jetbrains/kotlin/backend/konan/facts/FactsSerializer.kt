/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import java.nio.file.Path
import kotlin.collections.iterator
import kotlin.io.path.writeBytes

interface FactsSerializer {
    fun serialize(defs: DefsAssociation, uses: UsesAssociation): ByteArray
}

interface FactsDeserializer {
    fun deserialize(buffer: ByteArray): Pair<DefsAssociation, UsesAssociation>
}

/**
 * One line per entry: `relation  path  kind  symbolKey  hash  representation` (tab-separated, hex for the hashes).
 * The representation (e.g., the rendered signature) is only for humans and is not part of the key.
 */
object DebugFactsSerializer : FactsSerializer {
    private fun StringBuilder.appendFactLine(relation: String, path: FileName, key: FactKey, hash: FactHash) {
        append(relation).append('\t')
        append(path).append('\t')
        append(key.kind.name).append('\t')
        append(key.key.signatureHash.toString(16)).append('\t')
        append(hash.toString(16)).append('\t')
        append(key.representation.orEmpty()).append('\n')
    }

    private fun StringBuilder.appendFacts(relation: String, association: Map<FileName, Map<FactKey, FactHash>>) {
        for ([path, facts] in association) {
            for ([key, hash] in facts) appendFactLine(relation, path, key, hash)
        }
    }

    override fun serialize(defs: DefsAssociation, uses: UsesAssociation): ByteArray {
        val text = buildString {
            appendFacts("defines", defs)
            appendFacts("uses", uses)
        }
        return text.toByteArray(Charsets.UTF_8)
    }
}

object BinaryFactsSerializer : FactsSerializer {
    override fun serialize(defs: DefsAssociation, uses: UsesAssociation): ByteArray {
        TODO()
    }
}

internal fun FactsSerializer.serializeTo(path: Path, defs: DefsAssociation, uses: UsesAssociation) {
    path.writeBytes(serialize(defs, uses))
}