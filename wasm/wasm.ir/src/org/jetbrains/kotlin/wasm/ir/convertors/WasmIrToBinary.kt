/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.convertors

import org.jetbrains.kotlin.wasm.ir.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.wasm.ir.source.location.Box
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping

class WasmIrToBinary(
    outputStream: OutputStream,
    val module: WasmModule,
    val moduleName: String,
    val emitNameSection: Boolean,
    private val sourceMapFileName: String? = null,
    private val sourceLocationMappings: MutableList<SourceLocationMapping>? = null
) {
    private var b: ByteWriter = ByteWriter.OutputStream(outputStream)

    // "Stack" of offsets waiting initialization. 
    // Since blocks has as a prefix variable length number encoding its size we can't calculate absolute offsets inside those blocks 
    // until we generate whole block and generate size. So, we put them into "stack" and initialize as soo as we have all required data.
    private var offsets = persistentListOf<Box>()

    fun appendWasmModule() {
        b.writeUInt32(0x6d736100u) // WebAssembly magic
        b.writeUInt32(1u)          // version

        with(module) {
            // type section
            appendSection(1u) {
                val numRecGroups = if (recGroupTypes.isEmpty()) 0 else 1
                appendVectorSize(functionTypes.size + numRecGroups)
                functionTypes.forEach { appendFunctionTypeDeclaration(it) }
                if (!recGroupTypes.isEmpty()) {
                    b.writeByte(0x4f)
                    appendVectorSize(recGroupTypes.size)
                    recGroupTypes.forEach {
                        when (it) {
                            is WasmStructDeclaration -> appendStructTypeDeclaration(it)
                            is WasmArrayDeclaration -> appendArrayTypeDeclaration(it)
                            is WasmFunctionType -> appendFunctionTypeDeclaration(it)
                        }
                    }
                }
            }

            // import section
            appendSection(2u) {
                appendVectorSize(importsInOrder.size)
                importsInOrder.forEach {
                    when (it) {
                        is WasmFunction.Imported -> appendImportedFunction(it)
                        is WasmMemory -> appendMemory(it)
                        is WasmTable -> appendTable(it)
                        is WasmGlobal -> appendGlobal(it)
                        is WasmTag -> appendTag(it)
                        else -> error("Unknown import kind ${it::class}")
                    }
                }
            }

            // function section
            appendSection(3u) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach { appendDefinedFunction(it) }
            }

            // table section
            appendSection(4u) {
                appendVectorSize(tables.size)
                tables.forEach { appendTable(it) }
            }

            // memory section
            appendSection(5u) {
                appendVectorSize(memories.size)
                memories.forEach { appendMemory(it) }
            }

            // tag section
            appendSection(13u) {
                appendVectorSize(tags.size)
                tags.forEach { appendTag(it) }
            }

            appendSection(6u) {
                appendVectorSize(globals.size)
                globals.forEach { appendGlobal(it) }
            }

            appendSection(7u) {
                appendVectorSize(exports.size)
                exports.forEach { appendExport(it) }
            }

            if (startFunction != null) {
                appendSection(8u) {
                    appendStartFunction(startFunction)
                }
            }

            // element section
            appendSection(9u) {
                appendVectorSize(elements.size)
                elements.forEach { appendElement(it) }
            }

            if (dataCount) {
                appendSection(12u) {
                    b.writeVarUInt32(data.size.toUInt())
                }
            }

            // code section
            appendSection(10u) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach { appendCode(it) }
            }

            appendSection(11u) {
                appendVectorSize(data.size)
                data.forEach { appendData(it) }
            }

            // text section (should be placed after data)
            if (emitNameSection) {
                appendTextSection(definedFunctions)
            }

            if (sourceMapFileName != null) {
                // Custom section with URL to sourcemap
                appendSection(0u) {
                    b.writeString("sourceMappingURL")
                    b.writeString(sourceMapFileName)
                }
            }
        }
    }

    private fun appendTextSection(definedFunctions: List<WasmFunction.Defined>) {
        appendSection(0u) {
            b.writeString("name")
            appendSection(0u) {
                b.writeString(moduleName)
            }
            appendSection(1u) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach {
                    appendModuleFieldReference(it)
                    b.writeString(it.name)
                }
            }
            appendSection(2u) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach {
                    appendModuleFieldReference(it)
                    appendVectorSize(it.locals.size)
                    it.locals.forEach { local ->
                        b.writeVarUInt32(local.id)
                        b.writeString(local.name)
                    }
                }
            }

            // Extended Name Section
            // https://github.com/WebAssembly/extended-name-section/blob/main/document/core/appendix/custom.rst

            appendSection(4u) {
                appendVectorSize(module.recGroupTypes.size)
                module.recGroupTypes.forEach {
                    appendModuleFieldReference(it)
                    b.writeString(it.name)
                }
            }

            appendSection(7u) {
                appendVectorSize(module.globals.size)
                module.globals.forEach { global ->
                    appendModuleFieldReference(global)
                    b.writeString(global.name)
                }
            }

            // Experimental fields name section
            // https://github.com/WebAssembly/gc/issues/193
            appendSection(10u) {
                val structDeclarations = module.recGroupTypes.filterIsInstance<WasmStructDeclaration>()
                appendVectorSize(structDeclarations.size)
                structDeclarations.forEach {
                    appendModuleFieldReference(it)
                    appendVectorSize(it.fields.size)
                    it.fields.forEachIndexed { index, field ->
                        b.writeVarUInt32(index)
                        b.writeString(field.name)
                    }
                }
            }
        }
    }

    private fun appendInstr(instr: WasmInstr) {
        instr.location?.let {
            sourceLocationMappings?.add(SourceLocationMapping(offsets + Box(b.written), it))
        }

        val opcode = instr.operator.opcode

        if (opcode == WASM_OP_PSEUDO_OPCODE)
            return

        if (opcode > 0xFF) {
            b.writeByte((opcode ushr 8).toByte())
            b.writeByte((opcode and 0xFF).toByte())
        } else {
            b.writeByte(opcode.toByte())
        }

        instr.immediates.forEach {
            appendImmediate(it)
        }
    }

    private fun appendImmediate(x: WasmImmediate) {
        when (x) {
            is WasmImmediate.ConstI32 -> b.writeVarInt32(x.value)
            is WasmImmediate.ConstI64 -> b.writeVarInt64(x.value)
            is WasmImmediate.ConstF32 -> b.writeUInt32(x.rawBits)
            is WasmImmediate.ConstF64 -> b.writeUInt64(x.rawBits)
            is WasmImmediate.SymbolI32 -> b.writeVarInt32(x.value.owner)
            is WasmImmediate.MemArg -> {
                b.writeVarUInt32(x.align)
                b.writeVarUInt32(x.offset)
            }
            is WasmImmediate.BlockType -> appendBlockType(x)
            is WasmImmediate.FuncIdx -> appendModuleFieldReference(x.value.owner)
            is WasmImmediate.LocalIdx -> appendLocalReference(x.value.owner)
            is WasmImmediate.GlobalIdx -> appendModuleFieldReference(x.value.owner)
            is WasmImmediate.TypeIdx -> appendModuleFieldReference(x.value.owner)
            is WasmImmediate.MemoryIdx -> b.writeVarUInt32(x.value)
            is WasmImmediate.DataIdx -> b.writeVarUInt32(x.value.owner)
            is WasmImmediate.TableIdx -> b.writeVarUInt32(x.value.owner)
            is WasmImmediate.LabelIdx -> b.writeVarUInt32(x.value)
            is WasmImmediate.TagIdx -> b.writeVarUInt32(x.value)
            is WasmImmediate.LabelIdxVector -> {
                b.writeVarUInt32(x.value.size)
                for (target in x.value) {
                    b.writeVarUInt32(target)
                }
            }
            is WasmImmediate.ElemIdx -> appendModuleFieldReference(x.value)
            is WasmImmediate.ValTypeVector -> {
                b.writeVarUInt32(x.value.size)
                for (type in x.value) {
                    appendType(type)
                }
            }
            is WasmImmediate.GcType -> appendModuleFieldReference(x.value.owner)
            is WasmImmediate.StructFieldIdx -> b.writeVarUInt32(x.value.owner)
            is WasmImmediate.HeapType -> appendHeapType(x.value)
            is WasmImmediate.ConstString ->
                error("Instructions with pseudo immediates should be skipped")
        }
    }

    private fun appendSection(id: UShort, content: () -> Unit) {
        b.writeVarUInt7(id)
        withVarUInt32PayloadSizePrepended { content() }
    }

    private fun withVarUInt32PayloadSizePrepended(fn: () -> Unit) {
        val box = Box(-1)
        val previousOffsets = offsets
        offsets += box

        val previousWriter = b
        val newWriter = b.createTemp()
        b = newWriter
        fn()
        b = previousWriter
        b.writeVarUInt32(newWriter.written)

        box.value = b.written
        offsets = previousOffsets

        b.write(newWriter)
    }

    private fun appendVectorSize(size: Int) {
        b.writeVarUInt32(size)
    }

    private fun appendFunctionTypeDeclaration(type: WasmFunctionType) {
        b.writeVarInt7(-0x20)
        b.writeVarUInt32(type.parameterTypes.size)
        type.parameterTypes.forEach { appendType(it) }
        b.writeVarUInt32(type.resultTypes.size)
        type.resultTypes.forEach { appendType(it) }
    }

    private fun appendBlockType(type: WasmImmediate.BlockType) {
        when (type) {
            is WasmImmediate.BlockType.Function -> appendModuleFieldReference(type.type)
            is WasmImmediate.BlockType.Value -> when (type.type) {
                null -> b.writeVarInt7(-0x40)
                else -> appendType(type.type)
            }
        }
    }

    private fun appendFiledType(field: WasmStructFieldDeclaration) {
        appendType(field.type)
        b.writeVarUInt1(field.isMutable)
    }

    private fun appendStructTypeDeclaration(type: WasmStructDeclaration) {
        val superType = type.superType
        if (superType != null) {
            b.writeVarInt7(-0x30)
            appendVectorSize(1)
            appendModuleFieldReference(superType.owner)
        }
        b.writeVarInt7(-0x21)
        b.writeVarUInt32(type.fields.size)
        type.fields.forEach {
            appendFiledType(it)
        }
    }

    private fun appendArrayTypeDeclaration(type: WasmArrayDeclaration) {
        b.writeVarInt7(-0x22)
        appendFiledType(type.field)
    }

    val WasmFunctionType.index: Int
        get() = id!!

    private fun appendLimits(limits: WasmLimits) {
        b.writeVarUInt1(limits.maxSize != null)
        b.writeVarUInt32(limits.minSize)
        if (limits.maxSize != null)
            b.writeVarUInt32(limits.maxSize)
    }

    private fun appendImportedFunction(function: WasmFunction.Imported) {
        b.writeString(function.importPair.moduleName)
        b.writeString(function.importPair.declarationName)
        b.writeByte(0)  // Function external kind.
        b.writeVarUInt32(function.type.owner.index)
    }

    private fun appendDefinedFunction(function: WasmFunction.Defined) {
        b.writeVarUInt32(function.type.owner.index)
    }

    private fun appendTable(table: WasmTable) {
        if (table.importPair != null) {
            b.writeString(table.importPair.moduleName)
            b.writeString(table.importPair.declarationName)
            b.writeByte(1)
        }

        appendType(table.elementType)
        appendLimits(table.limits)
    }

    private fun appendMemory(memory: WasmMemory) {
        if (memory.importPair != null) {
            b.writeString(memory.importPair.moduleName)
            b.writeString(memory.importPair.declarationName)
            b.writeByte(2)
        }
        appendLimits(memory.limits)
    }

    private fun appendGlobal(c: WasmGlobal) {
        if (c.importPair != null) {
            b.writeString(c.importPair.moduleName)
            b.writeString(c.importPair.declarationName)
            b.writeByte(3)
            appendType(c.type)
            b.writeVarUInt1(c.isMutable)
            return
        }
        appendType(c.type)
        b.writeVarUInt1(c.isMutable)
        appendExpr(c.init, SourceLocation.TBDLocation)
    }

    private fun appendTag(t: WasmTag) {
        if (t.importPair != null) {
            b.writeString(t.importPair.moduleName)
            b.writeString(t.importPair.declarationName)
            b.writeByte(4)
            return
        }
        b.writeByte(0) // attribute
        assert(t.type.id != null) { "Unlinked tag id" }
        b.writeVarUInt32(t.type.id!!)
    }

    private fun appendExpr(expr: Iterable<WasmInstr>, location: SourceLocation) {
        expr.forEach { appendInstr(it) }
        appendInstr(WasmInstrWithLocation(WasmOp.END, location))
    }

    private fun appendExport(export: WasmExport<*>) {
        b.writeString(export.name)
        b.writeByte(export.kind)
        appendModuleFieldReference(export.field)
    }

    private fun appendStartFunction(startFunction: WasmFunction) {
        appendModuleFieldReference(startFunction)
    }

    private fun appendElement(element: WasmElement) {
        val isFuncIndices = element.values.all { it is WasmTable.Value.Function } && element.type == WasmFuncRef

        val funcIndices = if (isFuncIndices) {
            element.values.map { (it as WasmTable.Value.Function).function.owner.id!! }
        } else null

        fun writeElements() {
            appendVectorSize(element.values.size)
            if (funcIndices != null) {
                funcIndices.forEach { b.writeVarUInt32(it) }
            } else {
                element.values.forEach {
                    appendExpr((it as WasmTable.Value.Expression).expr, SourceLocation.TBDLocation)
                }
            }
        }

        fun writeTypeOrKind() {
            if (isFuncIndices) {
                b.writeByte(0x00)
            } else {
                appendType(element.type)
            }
        }

        when (val mode = element.mode) {
            WasmElement.Mode.Passive -> {
                b.writeByte(if (isFuncIndices) 0x01 else 0x05)
                writeTypeOrKind()
                writeElements()
            }
            is WasmElement.Mode.Active -> {
                val tableId = mode.table.id!!
                when {
                    tableId == 0 && isFuncIndices -> {
                        b.writeByte(0x0)
                        appendExpr(mode.offset, SourceLocation.TBDLocation)
                    }
                    isFuncIndices -> {
                        b.writeByte(0x2)
                        appendModuleFieldReference(mode.table)
                        appendExpr(mode.offset, SourceLocation.TBDLocation)
                        writeTypeOrKind()
                    }
                    else -> {
                        b.writeByte(0x6)
                        appendModuleFieldReference(mode.table)
                        appendExpr(mode.offset, SourceLocation.TBDLocation)
                        writeTypeOrKind()
                    }
                }
                writeElements()
            }
            WasmElement.Mode.Declarative -> {
                b.writeByte(if (isFuncIndices) 0x03 else 0x07)
                writeTypeOrKind()
                writeElements()
            }
        }
    }

    private fun appendCode(function: WasmFunction.Defined) {
        withVarUInt32PayloadSizePrepended {
            b.writeVarUInt32(function.locals.count { !it.isParameter })
            function.locals.forEach { local ->
                if (!local.isParameter) {
                    b.writeVarUInt32(1u)
                    appendType(local.type)
                }
            }

            appendExpr(function.instructions, SourceLocation.TBDLocation)
        }
    }

    private fun appendData(wasmData: WasmData) {
        when (val mode = wasmData.mode) {
            is WasmDataMode.Active -> {
                if (mode.memoryIdx == 0) {
                    b.writeByte(0)
                } else {
                    b.writeByte(2)
                    b.writeVarUInt32(mode.memoryIdx)
                }
                appendExpr(mode.offset, SourceLocation.TBDLocation)
            }
            WasmDataMode.Passive -> b.writeByte(1)
        }

        b.writeVarUInt32(wasmData.bytes.size)
        b.writeBytes(wasmData.bytes)
    }

    fun appendHeapType(type: WasmHeapType) {
        val code: Int = when (type) {
            is WasmHeapType.Simple -> type.code.toInt()
            is WasmHeapType.Type -> type.type.owner.id!!
        }
        b.writeVarInt32(code)
    }

    fun appendType(type: WasmType) {
        b.writeVarInt7(type.code)

        if (type is WasmRefType) {
            appendHeapType(type.heapType)
        }
        if (type is WasmRefNullType) {
            appendHeapType(type.heapType)
        }
    }

    fun appendLocalReference(local: WasmLocal) {
        b.writeVarUInt32(local.id)
    }

    fun appendModuleFieldReference(field: WasmNamedModuleField) {
        val id = field.id ?: error("${field::class} ${field.name} ID is unlinked")
        b.writeVarUInt32(id)
    }

    fun ByteWriter.writeVarUInt32(v: Int) {
        this.writeVarUInt32(v.toUInt())
    }

    private fun ByteWriter.writeString(str: String) {
        val bytes = str.toByteArray()
        this.writeVarUInt32(bytes.size)
        this.writeBytes(bytes)
    }
}

abstract class ByteWriter {
    abstract val written: Int

    abstract fun write(v: ByteWriter)
    abstract fun writeByte(v: Byte)
    abstract fun writeBytes(v: ByteArray)
    abstract fun createTemp(): ByteWriter

    fun writeUInt32(v: UInt) {
        writeByte(v.toByte())
        writeByte((v shr 8).toByte())
        writeByte((v shr 16).toByte())
        writeByte((v shr 24).toByte())
    }

    fun writeUInt64(v: ULong) {
        writeByte(v.toByte())
        writeByte((v shr 8).toByte())
        writeByte((v shr 16).toByte())
        writeByte((v shr 24).toByte())
        writeByte((v shr 32).toByte())
        writeByte((v shr 40).toByte())
        writeByte((v shr 48).toByte())
        writeByte((v shr 56).toByte())
    }

    fun writeVarInt7(v: Byte) {
        writeSignedLeb128(v.toLong())
    }

    fun writeVarInt32(v: Int) {
        writeSignedLeb128(v.toLong())
    }

    fun writeVarInt64(v: Long) {
        writeSignedLeb128(v)
    }

    fun writeVarUInt1(v: Boolean) {
        writeUnsignedLeb128(if (v) 1u else 0u)
    }

    fun writeVarUInt7(v: UShort) {
        writeUnsignedLeb128(v.toUInt())
    }

    fun writeVarUInt32(v: UInt) {
        writeUnsignedLeb128(v)
    }

    private fun writeUnsignedLeb128(v: UInt) {
        // Taken from Android source, Apache licensed
        @Suppress("NAME_SHADOWING")
        var v = v
        var remaining = v shr 7
        while (remaining != 0u) {
            val byte = (v and 0x7fu) or 0x80u
            writeByte(byte.toByte())
            v = remaining
            remaining = remaining shr 7
        }
        val byte = v and 0x7fu
        writeByte(byte.toByte())
    }

    private fun writeSignedLeb128(v: Long) {
        // Taken from Android source, Apache licensed
        @Suppress("NAME_SHADOWING")
        var v = v
        var remaining = v shr 7
        var hasMore = true
        val end = if (v and Long.MIN_VALUE == 0L) 0L else -1L
        while (hasMore) {
            hasMore = remaining != end || remaining and 1 != (v shr 6) and 1
            val byte = ((v and 0x7f) or if (hasMore) 0x80 else 0).toInt()
            writeByte(byte.toByte())
            v = remaining
            remaining = remaining shr 7
        }
    }

    class OutputStream(val os: java.io.OutputStream) : ByteWriter() {
        override var written = 0; private set

        override fun write(v: ByteWriter) {
            if (v !is OutputStream || v.os !is ByteArrayOutputStream) error("Writer not created from createTemp")
            v.os.writeTo(os)
            written += v.os.size()
        }

        override fun writeByte(v: Byte) {
            os.write(v.toInt())
            written++
        }

        override fun writeBytes(v: ByteArray) {
            os.write(v)
            written += v.size
        }

        override fun createTemp() = OutputStream(ByteArrayOutputStream())
    }
}
