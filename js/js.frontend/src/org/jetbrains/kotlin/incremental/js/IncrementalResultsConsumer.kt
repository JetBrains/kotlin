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
    fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray)
}

class IncrementalResultsConsumerImpl : IncrementalResultsConsumer {
    lateinit var headerMetadata: ByteArray
        private set

    private val _packageParts = hashMapOf<File, TranslationResultValue>()
    val packageParts: Map<File, TranslationResultValue>
        get() = _packageParts

    override fun processHeader(headerMetadata: ByteArray) {
        this.headerMetadata = headerMetadata
    }

    override fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray) {
        _packageParts.put(sourceFile, TranslationResultValue(packagePartMetadata, binaryAst))
    }
}

