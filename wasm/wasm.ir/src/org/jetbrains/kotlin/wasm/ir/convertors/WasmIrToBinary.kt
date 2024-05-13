/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir.convertors

import org.jetbrains.kotlin.wasm.ir.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.wasm.ir.debug.DebugData
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformation
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationConsumer
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGenerator
import org.jetbrains.kotlin.wasm.ir.source.location.*

private object WasmBinary {
    const val MAGIC = 0x6d736100u
    const val VERSION = 1u

    // https://webassembly.github.io/spec/core/binary/instructions.html#control-instructions
    const val EMPTY_TYPE_FOR_BLOCK: Byte = -0x40 // 0x40

    // TODO Change the link to final spec when it's merged
    // https://webassembly.github.io/gc/core/binary/types.html
    const val FUNC_TYPE: Byte = -0x20 // 0x60
    const val STRUCT_TYPE: Byte = -0x21 // 0x5F
    const val ARRAY_TYPE: Byte = -0x22 // 0x5E
    const val SUB_TYPE: Byte = -0x30 // 0x50
    const val SUB_FINAL_TYPE: Byte = -0x31 // 0x4F
    const val REC_GROUP: Byte = -0x32 // 0x4E

    @JvmInline
    value class Section private constructor(val id: UShort) {
        companion object {
            // https://webassembly.github.io/spec/core/binary/modules.html#sections
            val CUSTOM = Section(0u)
            val TYPE = Section(1u)
            val IMPORT = Section(2u)
            val FUNCTION = Section(3u)
            val TABLE = Section(4u)
            val MEMORY = Section(5u)
            val GLOBAL = Section(6u)
            val EXPORT = Section(7u)
            val START = Section(8u)
            val ELEMENT = Section(9u)
            val CODE = Section(10u)
            val DATA = Section(11u)
            val DATA_COUNT = Section(12u)
            val TAG = Section(13u)
        }
    }
}

class WasmIrToBinary(
    outputStream: OutputStream,
    val module: WasmModule,
    val moduleName: String,
    val emitNameSection: Boolean,
    private val debugInformationGenerator: DebugInformationGenerator? = null
) : DebugInformationConsumer {
    private var b: ByteWriter = ByteWriter.OutputStream(outputStream)

    // "Stack" of offsets waiting initialization. 
    // Since blocks have as a prefix variable length number encoding its size, we can't calculate absolute offsets inside those blocks
    // until we generate the whole block and generate size. So, we put them into "stack" and initialize as soon as we have all required data.
    private var offsets = persistentListOf<Box>()

    override fun consumeDebugInformation(debugInformation: DebugInformation) {
        debugInformation.forEach {
            appendSection(WasmBinary.Section.CUSTOM) {
                b.writeString(it.name)
                when (it.data) {
                    is DebugData.StringData -> b.writeString(it.data.value)
                }
            }
        }
    }

    fun appendWasmModule() {
        b.writeUInt32(WasmBinary.MAGIC)
        b.writeUInt32(WasmBinary.VERSION)

        with(module) {
            // type section
            appendSection(WasmBinary.Section.TYPE) {
                val numRecGroups = if (recGroupTypes.isEmpty()) 0 else 1
                appendVectorSize(functionTypes.size + numRecGroups)
                functionTypes.forEach { appendFunctionTypeDeclaration(it) }
                if (recGroupTypes.isNotEmpty()) {
                    b.writeVarInt7(WasmBinary.REC_GROUP)
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
            appendSection(WasmBinary.Section.IMPORT) {
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
            appendSection(WasmBinary.Section.FUNCTION) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach { appendDefinedFunction(it) }
            }

            // table section
            appendSection(WasmBinary.Section.TABLE) {
                appendVectorSize(tables.size)
                tables.forEach { appendTable(it) }
            }

            // memory section
            appendSection(WasmBinary.Section.MEMORY) {
                appendVectorSize(memories.size)
                memories.forEach { appendMemory(it) }
            }

            // tag section
            if (tags.isNotEmpty()) {
                appendSection(WasmBinary.Section.TAG) {
                    appendVectorSize(tags.size)
                    tags.forEach { appendTag(it) }
                }
            }

            appendSection(WasmBinary.Section.GLOBAL) {
                appendVectorSize(globals.size)
                globals.forEach { appendGlobal(it) }
            }

            appendSection(WasmBinary.Section.EXPORT) {
                appendVectorSize(exports.size)
                exports.forEach { appendExport(it) }
            }

            if (startFunction != null) {
                appendSection(WasmBinary.Section.START) {
                    appendStartFunction(startFunction)
                }
            }

            // element section
            appendSection(WasmBinary.Section.ELEMENT) {
                appendVectorSize(elements.size)
                elements.forEach { appendElement(it) }
            }

            if (dataCount) {
                appendSection(WasmBinary.Section.DATA_COUNT) {
                    b.writeVarUInt32(data.size.toUInt())
                }
            }

            // code section
            appendSection(WasmBinary.Section.CODE) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach { appendCode(it) }
            }

            appendSection(WasmBinary.Section.DATA) {
                appendVectorSize(data.size)
                data.forEach { appendData(it) }
            }

            // text section (should be placed after data)
            if (emitNameSection) {
                appendTextSection(definedFunctions)
            }

            debugInformationGenerator?.let { consumeDebugInformation(it.generateDebugInformation()) }
        }
    }

    private fun appendTextSection(definedFunctions: List<WasmFunction.Defined>) {
        appendSection(WasmBinary.Section.CUSTOM) {
            b.writeString("name")
            appendSection(WasmBinary.Section.CUSTOM) {
                b.writeString(moduleName)
            }
            appendSection(WasmBinary.Section.TYPE) {
                appendVectorSize(definedFunctions.size)
                definedFunctions.forEach {
                    appendModuleFieldReference(it)
                    b.writeString(it.name)
                }
            }
            appendSection(WasmBinary.Section.IMPORT) {
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

            appendSection(WasmBinary.Section.TABLE) {
                appendVectorSize(module.recGroupTypes.size)
                module.recGroupTypes.forEach {
                    appendModuleFieldReference(it)
                    b.writeString(it.name)
                }
            }

            appendSection(WasmBinary.Section.EXPORT) {
                appendVectorSize(module.globals.size)
                module.globals.forEach { global ->
                    appendModuleFieldReference(global)
                    b.writeString(global.name)
                }
            }

            // Experimental fields name section
            // https://github.com/WebAssembly/gc/issues/193
            appendSection(WasmBinary.Section.CODE) {
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
            debugInformationGenerator?.addSourceLocation(SourceLocationMappingToBinary(it, offsets + Box(b.written)))
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
            is WasmImmediate.ConstU8 -> b.writeUByte(x.value)
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
            is WasmImmediate.TagIdx -> b.writeVarUInt32(x.value.owner)
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

            is WasmImmediate.Catch -> {
                b.writeVarUInt32(x.type.opcode)
                x.immediates.forEach(this::appendImmediate)
            }
        }
    }

    private fun appendSection(section: WasmBinary.Section, content: () -> Unit) {
        b.writeVarUInt7(section.id)
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
        b.writeVarInt7(WasmBinary.FUNC_TYPE)
        b.writeVarUInt32(type.parameterTypes.size)
        type.parameterTypes.forEach { appendType(it) }
        b.writeVarUInt32(type.resultTypes.size)
        type.resultTypes.forEach { appendType(it) }
    }

    private fun appendBlockType(type: WasmImmediate.BlockType) {
        when (type) {
            is WasmImmediate.BlockType.Function -> appendModuleFieldReference(type.type)
            is WasmImmediate.BlockType.Value -> when (type.type) {
                null -> b.writeVarInt7(WasmBinary.EMPTY_TYPE_FOR_BLOCK)
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

        // https://webassembly.github.io/gc/core/binary/types.html#binary-subtype
        if (superType == null && type.isFinal) {
            // Short encoding form for final types without subtypes.
        } else {
            // General encoding
            b.writeVarInt7(
                if (type.isFinal)
                    WasmBinary.SUB_FINAL_TYPE
                else
                    WasmBinary.SUB_TYPE
            )

            if (superType != null) {
                appendVectorSize(1)
                appendModuleFieldReference(superType.owner)
            } else {
                appendVectorSize(0)
            }
        }

        b.writeVarInt7(WasmBinary.STRUCT_TYPE)
        b.writeVarUInt32(type.fields.size)
        type.fields.forEach {
            appendFiledType(it)
        }
    }

    private fun appendArrayTypeDeclaration(type: WasmArrayDeclaration) {
        b.writeVarInt7(WasmBinary.ARRAY_TYPE)
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
        b.writeString(function.importPair.declarationName.owner)
        b.writeByte(0)  // Function external kind.
        b.writeVarUInt32(function.type.owner.index)
    }

    private fun appendDefinedFunction(function: WasmFunction.Defined) {
        b.writeVarUInt32(function.type.owner.index)
    }

    private fun appendTable(table: WasmTable) {
        if (table.importPair != null) {
            b.writeString(table.importPair.moduleName)
            b.writeString(table.importPair.declarationName.owner)
            b.writeByte(1)
        }

        appendType(table.elementType)
        appendLimits(table.limits)
    }

    private fun appendMemory(memory: WasmMemory) {
        if (memory.importPair != null) {
            b.writeString(memory.importPair.moduleName)
            b.writeString(memory.importPair.declarationName.owner)
            b.writeByte(2)
        }
        appendLimits(memory.limits)
    }

    private fun appendGlobal(c: WasmGlobal) {
        if (c.importPair != null) {
            b.writeString(c.importPair.moduleName)
            b.writeString(c.importPair.declarationName.owner)
            b.writeByte(3)
            appendType(c.type)
            b.writeVarUInt1(c.isMutable)
            return
        }
        appendType(c.type)
        b.writeVarUInt1(c.isMutable)
        appendExpr(c.init)
    }

    private fun appendTag(t: WasmTag) {
        if (t.importPair != null) {
            b.writeString(t.importPair.moduleName)
            b.writeString(t.importPair.declarationName.owner)
            b.writeByte(4)
        }
        b.writeByte(0) // attribute
        assert(t.type.id != null) { "Unlinked tag id" }
        b.writeVarUInt32(t.type.id!!)
    }

    private fun appendExpr(expr: Iterable<WasmInstr>) {
        var skip = false
        var skipOnElse = false

        var currentTable: Array<List<WasmInstr>?>? = null
        var currentTableRow: MutableList<WasmInstr>? = null

        for (instruction in expr) {
            when (instruction.operator) {
                WasmOp.MACRO_IF -> {
                    check(!skip && !skipOnElse)
                    val ifParam = (instruction.immediates[0] as WasmImmediate.SymbolI32).value.owner
                    skip = ifParam == 0
                    skipOnElse = !skip
                }
                WasmOp.MACRO_ELSE -> {
                    skip = skipOnElse
                }
                WasmOp.MACRO_END_IF -> {
                    skip = false
                    skipOnElse = false
                }
                WasmOp.MACRO_TABLE -> {
                    check(currentTable == null && currentTableRow == null)
                    val tableSize = (instruction.immediates[0] as WasmImmediate.SymbolI32).value.owner
                    currentTable = arrayOfNulls(tableSize)
                }
                WasmOp.MACRO_TABLE_INDEX -> {
                    val indexParam = (instruction.immediates[0] as WasmImmediate.SymbolI32).value.owner
                    currentTableRow = mutableListOf()
                    currentTable!![indexParam] = currentTableRow
                }
                WasmOp.MACRO_TABLE_END -> {
                    currentTable!!.forEach { instructions ->
                        if (instructions == null) {
                            appendInstr(WasmInstrWithoutLocation(WasmOp.REF_NULL, listOf(WasmImmediate.HeapType(WasmRefNullrefType))))
                        } else {
                            instructions.forEach(::appendInstr)
                        }
                    }
                    currentTableRow = null
                    currentTable = null
                }
                else -> {
                    when {
                        skip -> {}
                        currentTableRow != null -> currentTableRow.add(instruction)
                        else -> appendInstr(instruction)
                    }
                }
            }
        }

        appendInstr(WasmInstrWithLocation(WasmOp.END, SourceLocation.NoLocation("End of instruction list")))
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
                    appendExpr((it as WasmTable.Value.Expression).expr)
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
                        appendExpr(mode.offset)
                    }
                    isFuncIndices -> {
                        b.writeByte(0x2)
                        appendModuleFieldReference(mode.table)
                        appendExpr(mode.offset)
                        writeTypeOrKind()
                    }
                    else -> {
                        b.writeByte(0x6)
                        appendModuleFieldReference(mode.table)
                        appendExpr(mode.offset)
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

            appendExpr(function.instructions)
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
                appendExpr(mode.offset)
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
        val id = field.id
            ?: error("${field::class} ${field.name} ID is unlinked")
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

    fun writeUByte(v: UByte) {
        writeByte(v.toByte())
    }

    fun writeUInt16(v: UShort) {
        writeByte(v.toByte())
        writeByte((v.toUInt() shr 8).toByte())
    }

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

private class SourceLocationMappingToBinary(
    override val sourceLocation: SourceLocation,
    // Offsets in generating binary, initialized lazily. Since blocks has as a prefix variable length number encoding its size
    // we can't calculate absolute offsets inside those blocks until we generate whole block and generate size.
    private val offsets: List<Box>,
) : SourceLocationMapping() {
    override val generatedLocation: SourceLocation.Location by lazy {
        SourceLocation.Location(
            module = "",
            file = "",
            line = 0,
            column = offsets.sumOf {
                assert(it.value >= 0) { "Offset must be >=0 but ${it.value}" }
                it.value
            }
        )
    }
}
