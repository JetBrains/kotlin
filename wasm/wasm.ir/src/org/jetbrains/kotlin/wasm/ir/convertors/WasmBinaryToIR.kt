/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.wasm.ir.convertors

import org.jetbrains.kotlin.wasm.ir.*
import java.nio.ByteBuffer


class WasmBinaryToIR(val b: MyByteReader) {
    val validVersion = 1u

    val functionTypes: MutableList<WasmFunctionType> = mutableListOf()
    val gcTypes: MutableList<WasmTypeDeclaration> = mutableListOf()

    val importsInOrder: MutableList<WasmNamedModuleField> = mutableListOf()
    val importedFunctions: MutableList<WasmFunction.Imported> = mutableListOf()
    val importedMemories: MutableList<WasmMemory> = mutableListOf()
    val importedTables: MutableList<WasmTable> = mutableListOf()
    val importedGlobals: MutableList<WasmGlobal> = mutableListOf()
    val importedTags: MutableList<WasmTag> = mutableListOf()

    val definedFunctions: MutableList<WasmFunction.Defined> = mutableListOf()
    val table: MutableList<WasmTable> = mutableListOf()
    val memory: MutableList<WasmMemory> = mutableListOf()
    val globals: MutableList<WasmGlobal> = mutableListOf()
    val exports: MutableList<WasmExport<*>> = mutableListOf()
    var startFunction: WasmFunction? = null
    val elements: MutableList<WasmElement> = mutableListOf()
    val data: MutableList<WasmData> = mutableListOf()
    var dataCount: Boolean = true
    val tags: MutableList<WasmTag> = mutableListOf()

    private fun <T> byIdx(l1: List<T>, l2: List<T>, index: Int): T {
        if (index < l1.size)
            return l1[index]
        return l2[index - l1.size]
    }

    private fun funByIdx(index: Int) = byIdx(importedFunctions, definedFunctions, index)
    private fun memoryByIdx(index: Int) = byIdx(importedMemories, memory, index)
    private fun elemByIdx(index: Int) = elements[index]
    private fun tableByIdx(index: Int) = byIdx(importedTables, table, index)
    private fun globalByIdx(index: Int) = byIdx(importedGlobals, globals, index)
    private fun tagByIdx(index: Int) = byIdx(importedTags, tags, index)

    fun parseModule(): WasmModule {
        if (b.readUInt32() != 0x6d736100u)
            error("InvalidMagicNumber")

        val version = b.readUInt32()
        if (version != validVersion)
            error("InvalidVersion(version.toLong(), listOf(validVersion.toLong()))")

        var maxSectionId = 0
        while (true) {
            val sectionId = try {
                b.readVarUInt7().toInt()
            } catch (e: Throwable) { // Unexpected end
                break
            }
            if (sectionId > 12) error("InvalidSectionId(sectionId)")
            require(sectionId == 12 || maxSectionId == 12 || sectionId == 0 || sectionId > maxSectionId) {
                "Section ID $sectionId came after $maxSectionId"
            }
            maxSectionId = maxOf(sectionId, maxSectionId)

            val sectionLength = b.readVarUInt32AsInt()
            b.limitSize(sectionLength, "Wasm section $sectionId of size $sectionLength") {
                when (sectionId) {
                    // Skip custom section
                    0 -> b.readBytes(sectionLength)

                    // Type section
                    1 -> {
                        forEachVectorElement {
                            when (val type = readTypeDeclaration()) {
                                is WasmFunctionType ->
                                    functionTypes += type
                                is WasmStructDeclaration ->
                                    gcTypes += type
                                is WasmArrayDeclaration -> {}
                            }
                        }
                    }

                    // Import section
                    2 -> {
                        forEachVectorElement {
                            val importPair = WasmImportDescriptor(readString(), WasmSymbol(readString()))
                            when (val kind = b.readByte().toInt()) {
                                0 -> {
                                    val type = functionTypes[b.readVarUInt32AsInt()]
                                    importedFunctions += WasmFunction.Imported(
                                        name = "",
                                        type = WasmSymbol(type),
                                        importPair = importPair,
                                    ).also { importsInOrder.add(it) }
                                }
                                // Table
                                1 -> {
                                    val elementType = readRefType()
                                    val limits = readLimits()
                                    importedTables.add(WasmTable(limits, elementType, importPair).also { importsInOrder.add(it) })
                                }
                                2 -> {
                                    val limits = readLimits()
                                    importedMemories.add(WasmMemory(limits, importPair).also { importsInOrder.add(it) })
                                }
                                3 -> {
                                    importedGlobals.add(
                                        WasmGlobal(
                                            name = "",
                                            type = readValueType(),
                                            isMutable = b.readVarUInt1(),
                                            init = emptyList(),
                                            importPair = importPair
                                        ).also { importsInOrder.add(it) }
                                    )
                                }
                                4 -> {
                                    val tag = readTag(importPair)
                                    importedTags.add(tag)
                                    importsInOrder.add(tag)
                                }
                                else -> error(
                                    "Unsupported import kind $kind"
                                )
                            }
                        }
                    }

                    // Function section
                    3 -> {
                        forEachVectorElement {
                            val functionType = functionTypes[b.readVarUInt32AsInt()]
                            definedFunctions.add(
                                WasmFunction.Defined(
                                    "",
                                    WasmSymbol(functionType),
                                    locals = functionType.parameterTypes.mapIndexed { index, wasmType ->
                                        WasmLocal(index, "", wasmType, true)
                                    }.toMutableList()
                                )
                            )
                        }
                    }


                    // Table section
                    4 -> {
                        forEachVectorElement {
                            val elementType = readRefType()
                            val limits = readLimits()
                            table.add(
                                WasmTable(limits, elementType)
                            )
                        }
                    }

                    // Memory section
                    5 -> {
                        forEachVectorElement {
                            val limits = readLimits()
                            memory.add(WasmMemory(limits))
                        }
                    }

                    // Tag section
                    13 -> {
                        forEachVectorElement {
                            tags.add(readTag())
                        }
                    }

                    // Globals section
                    6 -> {
                        forEachVectorElement {
                            val expr = mutableListOf<WasmInstr>()
                            globals.add(
                                WasmGlobal(
                                    name = "",
                                    type = readValueType(),
                                    isMutable = b.readVarUInt1(),
                                    init = expr
                                )
                            )
                            readExpression(expr)
                        }
                    }

                    // Export section
                    7 -> {
                        forEachVectorElement {
                            val name = readString()
                            val kind = b.readByte().toInt()
                            val index = b.readVarUInt32AsInt()
                            exports.add(
                                when (kind) {
                                    0 -> WasmExport.Function(name, funByIdx(index))
                                    1 -> WasmExport.Table(name, tableByIdx(index))
                                    2 -> WasmExport.Memory(name, memoryByIdx(index))
                                    3 -> WasmExport.Global(name, globalByIdx(index))
                                    4 -> WasmExport.Tag(name, tagByIdx(index))
                                    else -> error("Invalid export kind $kind")
                                }
                            )
                        }
                    }

                    // Start section
                    8 -> {
                        require(startFunction == null) { "Start function is already defined" }
                        startFunction = funByIdx(b.readVarUInt32AsInt())
                    }

                    // Element section
                    9 -> {
                        forEachVectorElement {
                            val firstByte = b.readUByte().toInt()

                            val mode: WasmElement.Mode = when (firstByte) {
                                0, 4 -> {
                                    val offset = readExpression()
                                    WasmElement.Mode.Active(tableByIdx(0), offset)
                                }

                                1, 5 ->
                                    WasmElement.Mode.Passive

                                2, 6 -> {
                                    val tableIdx = b.readVarUInt32()
                                    val offset = readExpression()
                                    WasmElement.Mode.Active(tableByIdx(tableIdx.toInt()), offset)
                                }

                                3, 7 ->
                                    WasmElement.Mode.Declarative

                                else ->
                                    error("Invalid element first byte $firstByte")
                            }

                            val type = if (firstByte < 5) {
                                if (firstByte in 1..3) {
                                    val elemKind = b.readByte()
                                    require(elemKind == 0.toByte())
                                }
                                WasmFuncRef
                            } else {
                                readValueType()
                            }

                            val values: List<WasmTable.Value> = mapVector {
                                if (firstByte < 4) {
                                    WasmTable.Value.Function(funByIdx(b.readVarUInt32AsInt()))
                                } else {
                                    val exprBody = mutableListOf<WasmInstr>()
                                    readExpression(exprBody)
                                    WasmTable.Value.Expression(exprBody)
                                }
                            }

                            elements += WasmElement(
                                type,
                                values,
                                mode,
                            )
                        }
                    }

                    // Code section
                    10 -> {
                        forEachVectorElement { functionId ->
                            val function = definedFunctions[functionId.toInt()]
                            val size = b.readVarUInt32AsInt()
                            b.limitSize(size, "function body size") {
                                mapVector {
                                    val count = b.readVarUInt32AsInt()
                                    val valueType = readValueType()

                                    val firstLocalId =
                                        function.locals.lastOrNull()?.id?.plus(1) ?: 0

                                    repeat(count) { thisIdx ->
                                        function.locals.add(
                                            WasmLocal(
                                                firstLocalId + thisIdx,
                                                "",
                                                valueType,
                                                false
                                            )
                                        )
                                    }
                                }

                                readExpression(function.instructions, function.locals)
                            }
                        }
                    }

                    // Data section
                    11 -> {
                        forEachVectorElement {
                            val mode = when (val firstByte = b.readByte().toInt()) {
                                0 -> WasmDataMode.Active(0, readExpression())
                                1 -> WasmDataMode.Passive
                                2 -> WasmDataMode.Active(b.readVarUInt32AsInt(), readExpression())
                                else -> error("Unsupported data mode $firstByte")
                            }
                            val size = b.readVarUInt32AsInt()
                            val bytes = b.readBytes(size)
                            data += WasmData(mode, bytes)
                        }
                    }

                    // Data count section
                    12 -> {
                        b.readVarUInt32() // Data count
                        dataCount = true
                    }
                }
            }
        }

        return WasmModule(
            functionTypes = functionTypes,
            recGroupTypes = gcTypes,
            importsInOrder = importsInOrder,
            importedFunctions = importedFunctions,
            importedMemories = importedMemories,
            importedTables = importedTables,
            importedGlobals = importedGlobals,
            importedTags = importedTags,
            definedFunctions = definedFunctions,
            tables = table,
            memories = memory,
            globals = globals,
            exports = exports,
            startFunction = startFunction,
            elements = elements,
            data = data,
            dataCount = dataCount,
            tags = tags
        ).also {
            it.calculateIds()
        }
    }

    private fun readLimits(): WasmLimits {
        val hasMax = b.readVarUInt1()
        return WasmLimits(
            minSize = b.readVarUInt32(),
            maxSize = if (hasMax) b.readVarUInt32() else null
        )
    }

    private fun readTag(importPair: WasmImportDescriptor? = null): WasmTag {
        val attribute = b.readByte()
        check(attribute.toInt() == 0) { "as per spec" }
        val type = functionTypes[b.readVarUInt32AsInt()]
        return WasmTag(type, importPair)
    }

    private fun readExpression(): MutableList<WasmInstr> =
        mutableListOf<WasmInstr>().also { readExpression(it) }

    private fun readExpression(instructions: MutableList<WasmInstr>, locals: List<WasmLocal> = emptyList()) {
        var blockCount = 0
        while (true) {
            require(blockCount >= 0)
            val inst = readInstruction(locals)

            when (inst.operator) {
                WasmOp.END -> {
                    // Last instruction in expression is end.
                    if (blockCount == 0) {
                        return
                    }
                    blockCount--
                }
                WasmOp.BLOCK, WasmOp.LOOP, WasmOp.IF -> {
                    blockCount++
                }
                else -> {
                }
            }

            instructions.add(inst)
        }
    }

    private fun readInstruction(locals: List<WasmLocal>): WasmInstr {
        val firstByte = b.readByte().toUByte().toInt()
        val opcode = if (firstByte in twoByteOpcodes) {
            val secondByte = b.readByte().toUByte().toInt()
            (firstByte shl 8) + secondByte
        } else {
            firstByte
        }

        val op = opcodesToOp[opcode]
            ?: error("Wrong opcode 0x${opcode.toString(16)}")


        val immediates = op.immediates.map {
            when (it) {
                WasmImmediateKind.CONST_U8 -> WasmImmediate.ConstU8(b.readUByte())
                WasmImmediateKind.CONST_I32 -> WasmImmediate.ConstI32(b.readVarInt32())
                WasmImmediateKind.CONST_I64 -> WasmImmediate.ConstI64(b.readVarInt64())
                WasmImmediateKind.CONST_F32 -> WasmImmediate.ConstF32(b.readUInt32())
                WasmImmediateKind.CONST_F64 -> WasmImmediate.ConstF64(b.readUInt64())

                WasmImmediateKind.MEM_ARG -> {
                    WasmImmediate.MemArg(
                        align = b.readVarUInt32(),
                        offset = b.readVarUInt32()
                    )
                }
                WasmImmediateKind.BLOCK_TYPE -> readBlockType()
                WasmImmediateKind.FUNC_IDX -> WasmImmediate.FuncIdx(funByIdx(b.readVarUInt32AsInt()))
                WasmImmediateKind.LOCAL_IDX -> WasmImmediate.LocalIdx(locals[b.readVarUInt32AsInt()])
                WasmImmediateKind.GLOBAL_IDX -> WasmImmediate.GlobalIdx(globalByIdx(b.readVarUInt32AsInt()))
                WasmImmediateKind.TYPE_IDX -> WasmImmediate.TypeIdx(functionTypes[b.readVarUInt32AsInt()])
                WasmImmediateKind.MEMORY_IDX -> WasmImmediate.MemoryIdx(b.readVarUInt32AsInt())
                WasmImmediateKind.DATA_IDX -> WasmImmediate.DataIdx(b.readVarUInt32AsInt())
                WasmImmediateKind.TABLE_IDX -> WasmImmediate.TableIdx(b.readVarUInt32AsInt())
                WasmImmediateKind.LABEL_IDX -> WasmImmediate.LabelIdx(b.readVarUInt32AsInt())
                WasmImmediateKind.TAG_IDX -> WasmImmediate.TagIdx(b.readVarUInt32AsInt())
                WasmImmediateKind.LABEL_IDX_VECTOR -> WasmImmediate.LabelIdxVector(mapVector { b.readVarUInt32AsInt() })
                WasmImmediateKind.ELEM_IDX -> WasmImmediate.ElemIdx(elemByIdx(b.readVarUInt32AsInt()))
                WasmImmediateKind.VAL_TYPE_VECTOR -> WasmImmediate.ValTypeVector(mapVector { readValueType() })
                WasmImmediateKind.STRUCT_TYPE_IDX -> TODO()
                WasmImmediateKind.STRUCT_FIELD_IDX -> TODO()
                WasmImmediateKind.TYPE_IMM -> TODO()
                WasmImmediateKind.HEAP_TYPE -> WasmImmediate.HeapType(readRefType())
                WasmImmediateKind.LOCAL_DEFS -> TODO()
                WasmImmediateKind.CATCH_VECTOR -> TODO()
            }
        }

        // We don't need location in Binary -> WasmIR, yet.
        return WasmInstrWithoutLocation(op, immediates)
    }

    private fun readTypeDeclaration(): WasmTypeDeclaration {
        when (b.readVarInt7()) {
            (-0x20).toByte() -> {
                val types = mapVector { readValueType() }
                val returnTypes = mapVector { readValueType() }
                return WasmFunctionType(types, returnTypes)
            }

            else -> TODO()
        }
    }

    private val codeToSimpleValueType: Map<Byte, WasmType> = listOf(
        WasmI32,
        WasmI64,
        WasmF32,
        WasmF64,
        WasmV128,
        WasmI8,
        WasmI16,
        WasmFuncRef,
        WasmAnyRef,
        WasmExternRef,
        WasmEqRef
    ).associateBy { it.code }

    private fun readValueType(): WasmType {
        val code = b.readVarInt7()
        return readValueTypeImpl(code)
    }

    private fun readBlockType(): WasmImmediate.BlockType {
        val code = b.readVarInt64()
        return when {
            code >= 0 -> WasmImmediate.BlockType.Function(functionTypes[code.toInt()])
            code == -0x40L -> WasmImmediate.BlockType.Value(null)
            else -> WasmImmediate.BlockType.Value(readValueTypeImpl(code.toByte()))
        }
    }

    private fun readRefType(): WasmType {
        val code = b.readByte()

        return when (code.toInt()) {
            0x70 -> WasmFuncRef
            0x6F -> WasmExternRef
            else -> error("Unsupported heap type ${code.toString(16)}")
        }
    }


    private fun readValueTypeImpl(code: Byte): WasmType {
        codeToSimpleValueType[code]?.let {
            return it
        }

        error("InvalidType 0x${code.toString(16)}")
    }

    private inline fun forEachVectorElement(block: (index: UInt) -> Unit) {
        val size = b.readVarUInt32()
        for (index in 0u until size) {
            block(index)
        }
    }

    private inline fun <T> mapVector(block: (index: UInt) -> T): List<T> {
        return (0u until b.readVarUInt32()).map { block(it) }
    }

    private fun MyByteReader.readVarUInt32AsInt() =
        this.readVarUInt32().toInt()

    fun readString() = b.readVarUInt32AsInt().let {
        // We have to use the decoder directly to get malformed-input errors
        Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(b.readBytes(it))).toString()
    }
}

class MyByteReader(val ins: java.io.InputStream) : ByteReader() {
    var offset: Long = 0

    class SizeLimit(val maxSize: Long, val reason: String)

    var sizeLimits = mutableListOf(SizeLimit(Long.MAX_VALUE, "Root"))
    var currentMaxSize: Long = Long.MAX_VALUE

    override val isEof: Boolean
        get() {
            error("Not implemented")
        }

    override fun read(amount: Int): ByteReader {
        error("Not implemented")
    }

    @OptIn(ExperimentalStdlibApi::class)
    inline fun limitSize(size: Int, reason: String, block: () -> Unit) {
        val maxSize = offset + size
        sizeLimits.add(SizeLimit(maxSize, reason))
        currentMaxSize = maxSize
        block()
        require(offset == currentMaxSize) {
            "Ending size-limited block \"$reason\". We haven't read all $size bytes."
        }
        sizeLimits.removeLast()
        currentMaxSize = sizeLimits.last().maxSize
    }

    override fun readByte(): Byte {
        val b = ins.read()
        if (b == -1)
            error("UnexpectedEnd")

        offset++
        if (offset > currentMaxSize) {
            error("Reading bytes past limit $currentMaxSize Reason: ${sizeLimits.last().reason}")
        }
        return b.toByte()
    }

    override fun readBytes(amount: Int?): ByteArray {
        require(amount != null)
        return ByteArray(amount) { readByte() }
    }
}

// First byte of two byte opcodes
val twoByteOpcodes: Set<Int> =
    opcodesToOp.keys.filter { it > 0xFF }.map { it ushr 8 }.toSet()


abstract class ByteReader {
    abstract val isEof: Boolean

    // Slices the next set off as its own and moves the position up that much
    abstract fun read(amount: Int): ByteReader
    abstract fun readByte(): Byte
    abstract fun readBytes(amount: Int? = null): ByteArray

    fun readUByte(): UByte =
        readByte().toUByte()

    fun readUInt32(): UInt =
        readUByte().toUInt() or
                (readUByte().toUInt() shl 8) or
                (readUByte().toUInt() shl 16) or
                (readUByte().toUInt() shl 24)

    fun readUInt64(): ULong =
        readUByte().toULong() or
                (readUByte().toULong() shl 8) or
                (readUByte().toULong() shl 16) or
                (readUByte().toULong() shl 24) or
                (readUByte().toULong() shl 32) or
                (readUByte().toULong() shl 40) or
                (readUByte().toULong() shl 48) or
                (readUByte().toULong() shl 56)


    fun readVarInt7() = readSignedLeb128().let {
        if (it < Byte.MIN_VALUE.toLong() || it > Byte.MAX_VALUE.toLong()) error("InvalidLeb128Number")
        it.toByte()
    }

    fun readVarInt32() = readSignedLeb128().let {
        if (it < Int.MIN_VALUE.toLong() || it > Int.MAX_VALUE.toLong()) error("InvalidLeb128Number")
        it.toInt()
    }

    fun readVarInt64() = readSignedLeb128(9)

    fun readVarUInt1() = readUnsignedLeb128().let {
        if (it != 1u && it != 0u) error("InvalidLeb128Number")
        it == 1u
    }

    fun readVarUInt7() = readUnsignedLeb128().let {
        if (it > 255u) error("InvalidLeb128Number")
        it.toShort()
    }

    fun readVarUInt32() = readUnsignedLeb128()

    protected fun readUnsignedLeb128(maxCount: Int = 4): UInt {
        // Taken from Android source, Apache licensed
        var result = 0u
        var cur: UInt
        var count = 0
        do {
            cur = readUByte().toUInt() and 0xffu
            result = result or ((cur and 0x7fu) shl (count * 7))
            count++
        } while (cur and 0x80u == 0x80u && count <= maxCount)
        if (cur and 0x80u == 0x80u) error("InvalidLeb128Number")
        return result
    }

    private fun readSignedLeb128(maxCount: Int = 4): Long {
        // Taken from Android source, Apache licensed
        var result = 0L
        var cur: Int
        var count = 0
        var signBits = -1L
        do {
            cur = readByte().toInt() and 0xff
            result = result or ((cur and 0x7f).toLong() shl (count * 7))
            signBits = signBits shl 7
            count++
        } while (cur and 0x80 == 0x80 && count <= maxCount)
        if (cur and 0x80 == 0x80) error("InvalidLeb128Number")

        // Check for 64 bit invalid, taken from Apache/MIT licensed:
        //  https://github.com/paritytech/parity-wasm/blob/2650fc14c458c6a252c9dc43dd8e0b14b6d264ff/src/elements/primitives.rs#L351
        // TODO: probably need 32 bit checks too, but meh, not in the suite
        if (count > maxCount && maxCount == 9) {
            if (cur and 0b0100_0000 == 0b0100_0000) {
                if ((cur or 0b1000_0000).toByte() != (-1).toByte()) error("InvalidLeb128Number")
            } else if (cur != 0) {
                error("InvalidLeb128Number")
            }
        }

        if ((signBits shr 1) and result != 0L) result = result or signBits
        return result
    }
}
