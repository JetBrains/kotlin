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
import java.security.MessageDigest

interface IncrementalResultsConsumer {
    /** processes new header metadata (serialized [JsProtoBuf.Header]) */
    fun processHeader(headerMetadata: ByteArray)
    /** processes new package part metadata and binary tree for compiled source file */
    fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray)
    /** [inlineFunction] is expected to be a body of inline function (an instance of [JsNode]),
     * but [Any] is used to avoid classloader conflicts in tests where the compiler is isolated
     * (such as [JsProtoComparisonTestGenerated])
     */
    fun processInlineFunction(sourceFile: File, fqName: String, inlineFunction: Any)
}

class IncrementalResultsConsumerImpl : IncrementalResultsConsumer {
    lateinit var headerMetadata: ByteArray
        private set

    private val _packageParts = hashMapOf<File, TranslationResultValue>()
    val packageParts: Map<File, TranslationResultValue>
        get() = _packageParts

    private val _inlineFuncs = hashMapOf<File, MutableMap<String, Any>>()
    val inlineFunctions: Map<File, Map<String, Long>>
        get() {
            val result = HashMap<File, Map<String, Long>>(_inlineFuncs.size)

            for ((file, inlineFnsFromFile) in _inlineFuncs) {
                val functionsHashes = HashMap<String, Long>(inlineFnsFromFile.size)

                for ((fqName, expression) in inlineFnsFromFile) {
                    functionsHashes[fqName] = expression.toString().toByteArray().md5()
                }

                result[file] = functionsHashes
            }

            return result
        }

    override fun processHeader(headerMetadata: ByteArray) {
        this.headerMetadata = headerMetadata
    }

    override fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray) {
        _packageParts.put(sourceFile, TranslationResultValue(packagePartMetadata, binaryAst))
    }

    override fun processInlineFunction(sourceFile: File, fqName: String, inlineFunction: Any) {
        val mapForSource = _inlineFuncs.getOrPut(sourceFile) { hashMapOf() }
        mapForSource[fqName] = inlineFunction
    }

    private fun ByteArray.md5(): Long {
        val d = MessageDigest.getInstance("MD5").digest(this)!!
        return ((d[0].toLong() and 0xFFL)
                or ((d[1].toLong() and 0xFFL) shl 8)
                or ((d[2].toLong() and 0xFFL) shl 16)
                or ((d[3].toLong() and 0xFFL) shl 24)
                or ((d[4].toLong() and 0xFFL) shl 32)
                or ((d[5].toLong() and 0xFFL) shl 40)
                or ((d[6].toLong() and 0xFFL) shl 48)
                or ((d[7].toLong() and 0xFFL) shl 56))
    }
}

