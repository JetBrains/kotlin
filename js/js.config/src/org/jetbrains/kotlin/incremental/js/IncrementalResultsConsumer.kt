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

import java.io.File

interface IncrementalResultsConsumer {
    /** processes new header metadata (serialized [JsProtoBuf.Header]) */
    fun processHeader(headerMetadata: ByteArray)

    /** processes new package part metadata and binary tree for compiled source file */
    fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray, inlineData: ByteArray)

    fun processPackageMetadata(packageName: String, metadata: ByteArray)

    fun processIrFile(
        sourceFile: File,
        fileData: ByteArray,
        types: ByteArray,
        signatures: ByteArray,
        strings: ByteArray,
        declarations: ByteArray,
        bodies: ByteArray,
        fqn: ByteArray,
        fileMetadata: ByteArray,
        debugInfo: ByteArray?,
        fileEntries: ByteArray,
    )
}

interface IncrementalNextRoundChecker {
    fun checkProtoChanges(sourceFile: File, packagePartMetadata: ByteArray)
    fun shouldGoToNextRound(): Boolean
}

open class IncrementalResultsConsumerImpl : IncrementalResultsConsumer {
    lateinit var headerMetadata: ByteArray
        private set

    private val _packageParts = hashMapOf<File, TranslationResultValue>()
    val packageParts: Map<File, TranslationResultValue>
        get() = _packageParts

    override fun processHeader(headerMetadata: ByteArray) {
        this.headerMetadata = headerMetadata
    }

    override fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray, inlineData: ByteArray) {
        _packageParts.put(sourceFile, TranslationResultValue(packagePartMetadata, binaryAst, inlineData))
    }

    private val _packageMetadata = hashMapOf<String, ByteArray>()
    val packageMetadata: Map<String, ByteArray>
        get() = _packageMetadata

    override fun processPackageMetadata(packageName: String, metadata: ByteArray) {
        _packageMetadata[packageName] = metadata
    }

//    class IrFileData(fileData: ByteArray, symbols: ByteArray, types: ByteArray, strings: ByteArray, bodies: ByteArray, declarations: ByteArray)
    private val _irFileData = hashMapOf<File, IrTranslationResultValue>()
    val irFileData: Map<File, IrTranslationResultValue>
        get() = _irFileData

    override fun processIrFile(
        sourceFile: File,
        fileData: ByteArray,
        types: ByteArray,
        signatures: ByteArray,
        strings: ByteArray,
        declarations: ByteArray,
        bodies: ByteArray,
        fqn: ByteArray,
        fileMetadata: ByteArray,
        debugInfo: ByteArray?,
        fileEntries: ByteArray,
    ) {
        _irFileData[sourceFile] = IrTranslationResultValue(fileData, types, signatures, strings, declarations, bodies, fqn, fileMetadata, debugInfo, fileEntries)
    }
}
