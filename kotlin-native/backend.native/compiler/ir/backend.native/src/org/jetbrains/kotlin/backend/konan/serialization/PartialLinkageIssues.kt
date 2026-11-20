/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageIssueSignificance
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageIssueSink
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogger

/**
 * A partial linkage issue as it is stored in an incremental compilation cache: the rendered message plus everything
 * that is needed to report the issue once again in a compilation that reuses the cached code (see KT-78253).
 *
 * Note that the severity of the issue is not stored: it is derived from [significance] and from the partial linkage
 * log level that is effective in the compilation which reports the issue.
 */
data class SerializedPartialLinkageIssue(
        val moduleName: String,
        val filePath: String,
        val lineNumber: Int,
        val columnNumber: Int,
        val message: String,
        val significance: PartialLinkageIssueSignificance,
) {
    val location: PartialLinkageLogger.Location
        get() = PartialLinkageLogger.Location(moduleName, filePath, lineNumber, columnNumber)
}

/**
 * Collects the partial linkage issues that the PL engine has logged during the current compilation, so that they can
 * be stored in the incremental compilation caches being built (see [org.jetbrains.kotlin.backend.konan.CacheStorage]).
 */
class PartialLinkageIssueCollector : PartialLinkageIssueSink {
    private val issues = mutableListOf<SerializedPartialLinkageIssue>()

    override fun record(
            message: String,
            location: PartialLinkageLogger.Location,
            significance: PartialLinkageIssueSignificance,
    ) {
        issues += SerializedPartialLinkageIssue(
                moduleName = location.moduleName,
                filePath = location.filePath,
                lineNumber = location.lineNumber,
                columnNumber = location.columnNumber,
                message = message,
                significance = significance,
        )
    }

    fun collectResult(): List<SerializedPartialLinkageIssue> = issues
}

internal object PartialLinkageIssuesSerializer {
    private const val NUM_SERIALIZED_ENTRIES: Int = 6

    fun serialize(issues: List<SerializedPartialLinkageIssue>): ByteArray {
        val stringTable = buildStringTable {
            issues.forEach {
                +it.moduleName
                +it.filePath
                +it.message
            }
        }
        val size = stringTable.sizeBytes + issues.size * Int.SIZE_BYTES * NUM_SERIALIZED_ENTRIES
        val stream = ByteArrayStream(ByteArray(size))
        stringTable.serialize(stream)
        issues.forEach {
            stream.writeInt(stringTable.indices[it.moduleName]!!)
            stream.writeInt(stringTable.indices[it.filePath]!!)
            stream.writeInt(it.lineNumber)
            stream.writeInt(it.columnNumber)
            stream.writeInt(stringTable.indices[it.message]!!)
            stream.writeInt(it.significance.ordinal)
        }
        return stream.buf
    }

    fun deserializeTo(data: ByteArray, result: MutableList<SerializedPartialLinkageIssue>) {
        val stream = ByteArrayStream(data)
        val stringTable = StringTable.deserialize(stream)
        while (stream.hasData()) {
            val moduleName = stringTable[stream.readInt()]
            val filePath = stringTable[stream.readInt()]
            val lineNumber = stream.readInt()
            val columnNumber = stream.readInt()
            val message = stringTable[stream.readInt()]
            val significance = PartialLinkageIssueSignificance.entries[stream.readInt()]
            result.add(
                    SerializedPartialLinkageIssue(moduleName, filePath, lineNumber, columnNumber, message, significance)
            )
        }
    }
}
