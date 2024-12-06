/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IdSignatureSerializer
import org.jetbrains.kotlin.backend.common.serialization.IrInterningService
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFile
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.library.impl.IrArrayMemoryReader
import org.jetbrains.kotlin.library.impl.IrMemoryArrayWriter
import org.jetbrains.kotlin.library.impl.IrMemoryStringWriter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature

internal object InlineFunctionBodyReferenceSerializer {
    fun serialize(bodies: List<SerializedInlineFunctionReference>): ByteArray {
        val stringTable = buildStringTable {
            bodies.forEach {
                +it.file.fqName
                +it.file.path
            }
        }
        val size = stringTable.sizeBytes + bodies.sumOf {
            Int.SIZE_BYTES * (12 + it.outerReceiverSigs.size + it.valueParameterSigs.size + it.typeParameterSigs.size + it.defaultValues.size)
        }
        val stream = ByteArrayStream(ByteArray(size))
        stringTable.serialize(stream)
        bodies.forEach {
            stream.writeInt(stringTable.indices[it.file.fqName]!!)
            stream.writeInt(stringTable.indices[it.file.path]!!)
            stream.writeInt(it.functionSignature)
            stream.writeInt(it.body)
            stream.writeInt(it.startOffset)
            stream.writeInt(it.endOffset)
            stream.writeInt(it.extensionReceiverSig)
            stream.writeInt(it.dispatchReceiverSig)
            stream.writeIntArray(it.outerReceiverSigs)
            stream.writeIntArray(it.valueParameterSigs)
            stream.writeIntArray(it.typeParameterSigs)
            stream.writeIntArray(it.defaultValues)
        }
        return stream.buf
    }

    fun deserializeTo(data: ByteArray, result: MutableList<SerializedInlineFunctionReference>) {
        val stream = ByteArrayStream(data)
        val stringTable = StringTable.deserialize(stream)
        while (stream.hasData()) {
            val fileFqName = stringTable[stream.readInt()]
            val filePath = stringTable[stream.readInt()]
            val functionSignature = stream.readInt()
            val body = stream.readInt()
            val startOffset = stream.readInt()
            val endOffset = stream.readInt()
            val extensionReceiverSig = stream.readInt()
            val dispatchReceiverSig = stream.readInt()
            val outerReceiverSigs = stream.readIntArray()
            val valueParameterSigs = stream.readIntArray()
            val typeParameterSigs = stream.readIntArray()
            val defaultValues = stream.readIntArray()
            result.add(
                SerializedInlineFunctionReference(
                    SerializedFileReference(fileFqName, filePath), functionSignature, body, startOffset, endOffset,
                    extensionReceiverSig, dispatchReceiverSig, outerReceiverSigs, valueParameterSigs,
                    typeParameterSigs, defaultValues
                )
            )
        }
    }
}

internal object ClassFieldsSerializer {

    fun serialize(classFields: List<SerializedClassFields>): ByteArray {

        val protoStringMap = hashMapOf<String, Int>()
        val protoStringArray = arrayListOf<String>()
        val protoIdSignatureMap = mutableMapOf<IdSignature, Int>()
        val protoIdSignatureArray = arrayListOf<ProtoIdSignature>()

        fun serializeString(value: String): Int = protoStringMap.getOrPut(value) {
            protoStringArray.add(value)
            protoStringArray.size - 1
        }

        val idSignatureSerializer = IdSignatureSerializer(
            ::serializeString,
            ::serializeString,
            protoIdSignatureMap,
            protoIdSignatureArray
        )

        classFields.forEach {
            idSignatureSerializer.protoIdSignature(it.classSignature)
        }
        val signatures = IrMemoryArrayWriter(protoIdSignatureArray.map { it.toByteArray() }).writeIntoMemory()
        val signatureStrings = IrMemoryStringWriter(protoStringArray).writeIntoMemory()
        val stringTable = buildStringTable {
            classFields.forEach {
                +it.file.fqName
                +it.file.path
                it.fields.forEach { +it.name }
            }
        }
        val size = stringTable.sizeBytes + classFields.sumOf { Int.SIZE_BYTES * (6 + it.typeParameterSigs.size + it.fields.size * 4) }
        val stream = ByteArrayStream(ByteArray(size))
        stringTable.serialize(stream)
        classFields.forEach {
            stream.writeInt(stringTable.indices[it.file.fqName]!!)
            stream.writeInt(stringTable.indices[it.file.path]!!)
            stream.writeInt(protoIdSignatureMap[it.classSignature]!!)
            stream.writeIntArray(it.typeParameterSigs)
            stream.writeInt(it.outerThisIndex)
            stream.writeInt(it.fields.size)
            it.fields.forEach { field ->
                stream.writeInt(stringTable.indices[field.name]!!)
                stream.writeInt(field.binaryType)
                stream.writeInt(field.flags)
                stream.writeInt(field.alignment)
            }
        }
        return IrMemoryArrayWriter(listOf(signatures, signatureStrings, stream.buf)).writeIntoMemory()
    }

    fun deserializeTo(data: ByteArray, result: MutableList<SerializedClassFields>) {
        val reader = IrArrayMemoryReader(data)
        val signatures = IrArrayMemoryReader(reader.tableItemBytes(0))
        val signatureStrings = IrArrayMemoryReader(reader.tableItemBytes(1))
        val libFile: IrLibraryFile = object : IrLibraryFile() {
            override fun declaration(index: Int) = error("Declarations are not needed for IdSignature deserialization")
            override fun type(index: Int) = error("Types are not needed for IdSignature deserialization")
            override fun expressionBody(index: Int) = error("Expression bodies are not needed for IdSignature deserialization")
            override fun statementBody(index: Int) = error("Statement bodies are not needed for IdSignature deserialization")

            override fun signature(index: Int): ProtoIdSignature {
                val bytes = signatures.tableItemBytes(index)
                return ProtoIdSignature.parseFrom(bytes.codedInputStream)
            }

            private fun deserializeString(index: Int): String = WobblyTF8.decode(signatureStrings.tableItemBytes(index))

            override fun string(index: Int): String = deserializeString(index)
            override fun debugInfo(index: Int): String = deserializeString(index)
        }
        val interner = IrInterningService()
        val stream = ByteArrayStream(reader.tableItemBytes(2))
        val stringTable = StringTable.deserialize(stream)
        while (stream.hasData()) {
            val fileFqName = stringTable[stream.readInt()]
            val filePath = stringTable[stream.readInt()]
            val signatureIndex = stream.readInt()
            val typeParameterSigs = stream.readIntArray()
            val outerThisIndex = stream.readInt()
            val fieldsCount = stream.readInt()
            val fields = Array(fieldsCount) {
                val name = stringTable[stream.readInt()]
                val binaryType = stream.readInt()
                val flags = stream.readInt()
                val alignment = stream.readInt()
                SerializedClassFieldInfo(name, binaryType, flags, alignment)
            }
            val fileSignature = IdSignature.FileSignature(
                id = Any(),
                fqName = FqName(fileFqName),
                fileName = filePath
            )
            val idSignatureDeserializer = IdSignatureDeserializer(libFile, fileSignature, interner)
            val classSignature = idSignatureDeserializer.deserializeIdSignature(signatureIndex)
            result.add(
                SerializedClassFields(
                    SerializedFileReference(fileFqName, filePath), classSignature, typeParameterSigs, outerThisIndex, fields
                )
            )
        }
    }
}

internal object EagerInitializedPropertySerializer {
    fun serialize(properties: List<SerializedEagerInitializedFile>): ByteArray {
        val stringTable = buildStringTable {
            properties.forEach {
                +it.file.fqName
                +it.file.path
            }
        }
        val size = stringTable.sizeBytes + properties.sumOf { Int.SIZE_BYTES * 2 }
        val stream = ByteArrayStream(ByteArray(size))
        stringTable.serialize(stream)
        properties.forEach {
            stream.writeInt(stringTable.indices[it.file.fqName]!!)
            stream.writeInt(stringTable.indices[it.file.path]!!)
        }
        return stream.buf
    }

    fun deserializeTo(data: ByteArray, result: MutableList<SerializedEagerInitializedFile>) {
        val stream = ByteArrayStream(data)
        val stringTable = StringTable.deserialize(stream)
        while (stream.hasData()) {
            val fileFqName = stringTable[stream.readInt()]
            val filePath = stringTable[stream.readInt()]
            result.add(SerializedEagerInitializedFile(SerializedFileReference(fileFqName, filePath)))
        }
    }
}
