/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental.js

import org.jetbrains.kotlin.name.CallableId
import java.io.File

interface IncrementalResultsConsumer {
    /** processes new package part metadata and binary tree for compiled source file */
    fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray)

    fun processIrFile(
        sourceFile: File,
        fileData: ByteArray,
        types: ByteArray,
        signatures: ByteArray,
        strings: ByteArray,
        declarations: ByteArray,
        bodies: ByteArray,
        fqn: ByteArray,
        debugInfo: ByteArray?,
        fileEntries: ByteArray?,
    )

    fun processIrInlineFile(
        sourceFile: File,
        fileData: ByteArray,
        types: ByteArray,
        signatures: ByteArray,
        strings: ByteArray,
        declarations: ByteArray,
        bodies: ByteArray,
        fqn: ByteArray,
        debugInfo: ByteArray?,
        fileEntries: ByteArray?,
    )

    fun processIrInlineIds(
        sourceFile: File,
        ids: List<CallableId>,
    )
}

interface IncrementalNextRoundChecker {
    fun checkProtoChanges(sourceFile: File, packagePartMetadata: ByteArray)
    fun shouldGoToNextRound(): Boolean
}

open class IncrementalResultsConsumerImpl : IncrementalResultsConsumer {
    val packageParts: Map<File, TranslationResultValue>
        field = hashMapOf<File, TranslationResultValue>()

    override fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray) {
        packageParts.put(sourceFile, TranslationResultValue(packagePartMetadata))
    }

//    class IrFileData(fileData: ByteArray, symbols: ByteArray, types: ByteArray, strings: ByteArray, bodies: ByteArray, declarations: ByteArray)
    val irFileData: Map<File, IrTranslationResultValue>
        field = hashMapOf<File, IrTranslationResultValue>()

    override fun processIrFile(
        sourceFile: File,
        fileData: ByteArray,
        types: ByteArray,
        signatures: ByteArray,
        strings: ByteArray,
        declarations: ByteArray,
        bodies: ByteArray,
        fqn: ByteArray,
        debugInfo: ByteArray?,
        fileEntries: ByteArray?,
    ) {
        irFileData[sourceFile] = IrTranslationResultValue(
            fileData, types, signatures, strings, declarations, bodies, fqn, debugInfo, fileEntries
        )
    }

    val irInlineFileData: Map<File, IrTranslationResultValue>
        field = hashMapOf<File, IrTranslationResultValue>()

    override fun processIrInlineFile(
        sourceFile: File,
        fileData: ByteArray,
        types: ByteArray,
        signatures: ByteArray,
        strings: ByteArray,
        declarations: ByteArray,
        bodies: ByteArray,
        fqn: ByteArray,
        debugInfo: ByteArray?,
        fileEntries: ByteArray?,
    ) {
        irInlineFileData[sourceFile] = IrTranslationResultValue(
            fileData, types, signatures, strings, declarations, bodies, fqn, debugInfo, fileEntries
        )
    }

    val inlineFunctionRepresentationData: Map<File, List<IrInlineFunctionRepresentation>>
        field = hashMapOf<File, List<IrInlineFunctionRepresentation>>()

    override fun processIrInlineIds(
        sourceFile: File,
        ids: List<CallableId>,
    ) {
        if (ids.isEmpty()) return
        val fileHash = irInlineFileData[sourceFile]?.hash() ?: error("There is no inline data associated with $sourceFile")
        inlineFunctionRepresentationData[sourceFile] = buildMap {
            ids.forEach { callableId -> getOrPut(callableId) { mutableListOf() }.add(fileHash) }
        }.map { IrInlineFunctionRepresentation(it.key, it.value) }
    }

    private fun IrTranslationResultValue.hash(): Long {
        val hashCodes = listOfNotNull<Long>(
            this.fileData.contentHashCode().toLong(),
            this.types.contentHashCode().toLong(),
            this.signatures.contentHashCode().toLong(),
            this.strings.contentHashCode().toLong(),
            this.declarations.contentHashCode().toLong(),
            this.bodies.contentHashCode().toLong(),
            this.fqn.contentHashCode().toLong(),
            this.debugInfo?.contentHashCode()?.toLong(),
            this.fileEntries?.contentHashCode()?.toLong(),
        )
        return hashCodes.reduce { acc, hashCode -> acc * 31 + hashCode }
    }
}
