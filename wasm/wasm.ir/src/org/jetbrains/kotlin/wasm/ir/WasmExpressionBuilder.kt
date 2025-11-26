/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

internal fun WasmOp.isBlockStart(): Boolean = when (this) {
    WasmOp.BLOCK, WasmOp.LOOP, WasmOp.IF, WasmOp.TRY, WasmOp.TRY_TABLE -> true
    else -> false
}

internal fun WasmOp.isBlockEnd(): Boolean = this == WasmOp.END

/**
 * Class for building a wasm instructions list.
 *
 * Note in most of the methods, location is a required parameter, and it's expected to be passed explicitly.
 * The goals are:
 * - Avoid missing a location
 * - Avoid providing a wrong location
 *   - It's hard to achieve fully, but:
 *     - at least, an API user has to think about what to pass a location
 *     - it's not taken from some context-like thing implicitly, so you will not get it implicitly from a wrong context/scope.
 */
class WasmExpressionBuilder(
    val expression: MutableList<WasmInstr>,
    val skipCommentInstructions: Boolean = true,
    val skipLocations: Boolean = true,
) {
    private var _numberOfNestedBlocks = 0

    fun buildInstr(
        op: WasmOp,
        location: SourceLocation,
    ) {
        if (location == SourceLocation.NoLocation) {
            wasmInstrWithoutLocation(op)
        } else {
            check(!skipLocations) { "Unexpected location in skipLocation mode" }
            wasmInstrWithLocation(op, location)
        }.appendInstruction()
    }

    fun buildInstr(
        op: WasmOp,
        location: SourceLocation,
        immediate: WasmImmediate,
    ) {
        if (skipLocations || location == SourceLocation.NoLocation) {
            check(location !is SourceLocation.DefinedLocation)
            wasmInstrWithoutLocation(op, immediate)
        } else {
            check(!skipLocations) { "Unexpected location in skipLocation mode" }
            wasmInstrWithLocation(op, location, immediate)
        }.appendInstruction()
    }

    fun buildInstr(
        op: WasmOp,
        location: SourceLocation,
        immediate1: WasmImmediate,
        immediate2: WasmImmediate,
    ) {
        if (skipLocations || location == SourceLocation.NoLocation) {
            wasmInstrWithoutLocation(op, immediate1, immediate2)
        } else {
            check(!skipLocations) { "Unexpected location in skipLocation mode" }
            wasmInstrWithLocation(op, location, immediate1, immediate2)
        }.appendInstruction()
    }

    fun buildInstr(
        op: WasmOp, location: SourceLocation,
        immediate1: WasmImmediate,
        immediate2: WasmImmediate,
        immediate3: WasmImmediate,
    ) {
        if (skipLocations || location == SourceLocation.NoLocation) {
            wasmInstrWithoutLocation(op, immediate1, immediate2, immediate3)
        } else {
            check(!skipLocations) { "Unexpected location in skipLocation mode" }
            wasmInstrWithLocation(op, location, immediate1, immediate2, immediate3)
        }.appendInstruction()
    }

    fun buildInstr(
        op: WasmOp, location: SourceLocation,
        immediate1: WasmImmediate,
        immediate2: WasmImmediate,
        immediate3: WasmImmediate,
        immediate4: WasmImmediate,
    ) {
        if (skipLocations || location == SourceLocation.NoLocation) {
            wasmInstrWithoutLocation(op, immediate1, immediate2, immediate3, immediate4)
        } else {
            check(!skipLocations) { "Unexpected location in skipLocation mode" }
            wasmInstrWithLocation(op, location, immediate1, immediate2, immediate3, immediate4)
        }.appendInstruction()
    }

    val numberOfNestedBlocks: Int
        get() = _numberOfNestedBlocks

    private fun WasmInstr.appendInstruction() {
        if (operator.isBlockStart()) {
            _numberOfNestedBlocks++
        } else if (operator.isBlockEnd()) {
            _numberOfNestedBlocks--
        }
        expression += this
    }

    fun buildConstI32(value: Int, location: SourceLocation) {
        buildInstr(WasmOp.I32_CONST, location, WasmImmediate.ConstI32(value))
    }

    fun buildConstI64(value: Long, location: SourceLocation) {
        buildInstr(WasmOp.I64_CONST, location, WasmImmediate.ConstI64(value))
    }

    fun buildConstF32(value: Float, location: SourceLocation) {
        buildInstr(WasmOp.F32_CONST, location, WasmImmediate.ConstF32(value.toRawBits().toUInt()))
    }

    fun buildConstF64(value: Double, location: SourceLocation) {
        buildInstr(WasmOp.F64_CONST, location, WasmImmediate.ConstF64(value.toRawBits().toULong()))
    }

    fun buildConstI32Symbol(value: WasmSymbol<Int>, location: SourceLocation) {
        buildInstr(WasmOp.I32_CONST, location, WasmImmediate.SymbolI32(value))
    }

    fun buildUnreachable(location: SourceLocation) {
        buildInstr(WasmOp.UNREACHABLE, location)
    }

    @Suppress("UNUSED_PARAMETER")
    inline fun buildFunctionTypedBlock(label: String?, resultType: WasmSymbolReadOnly<WasmFunctionType>, body: (Int) -> Unit) {
        buildInstr(WasmOp.BLOCK, SourceLocation.NoLocation("BLOCK"), WasmImmediate.BlockType.Function(resultType))
        body(numberOfNestedBlocks)
        buildEnd()
    }

    @Suppress("UNUSED_PARAMETER")
    inline fun buildBlock(label: String?, resultType: WasmType? = null, body: (Int) -> Unit) {
        buildInstr(WasmOp.BLOCK, SourceLocation.NoLocation("BLOCK"), WasmImmediate.BlockType.Value(resultType))
        body(numberOfNestedBlocks)
        buildEnd()
    }

    @Suppress("UNUSED_PARAMETER")
    inline fun buildLoop(label: String?, resultType: WasmType? = null, body: (Int) -> Unit) {
        buildInstr(WasmOp.LOOP, SourceLocation.NoLocation("LOOP"), WasmImmediate.BlockType.Value(resultType))
        body(numberOfNestedBlocks)
        buildEnd()
    }


    @Suppress("UNUSED_PARAMETER")
    fun buildIf(label: String?, resultType: WasmType? = null) {
        buildInstr(WasmOp.IF, SourceLocation.NoLocation, WasmImmediate.BlockType.Value(resultType))
    }

    fun buildElse(location: SourceLocation? = null) {
        buildInstr(WasmOp.ELSE, location ?: SourceLocation.NoLocation)
    }

    fun buildBlock(resultType: WasmType? = null): Int {
        buildInstr(WasmOp.BLOCK, SourceLocation.NoLocation, WasmImmediate.BlockType.Value(resultType))
        return numberOfNestedBlocks
    }

    fun buildEnd(location: SourceLocation? = null) {
        buildInstr(WasmOp.END, location ?: SourceLocation.NoLocation)
    }

    fun buildBrInstr(brOp: WasmOp, absoluteBlockLevel: Int, location: SourceLocation) {
        val relativeLevel = numberOfNestedBlocks - absoluteBlockLevel
        assert(relativeLevel >= 0) { "Negative relative block index" }
        buildInstr(brOp, location, WasmImmediate.LabelIdx(relativeLevel))
    }

    fun buildBrOnCastInstr(
        brOp: WasmOp,
        absoluteBlockLevel: Int,
        fromIsNullable: Boolean,
        toIsNullable: Boolean,
        from: WasmHeapType,
        to: WasmHeapType,
        location: SourceLocation,
    ) {
        val relativeLevel = numberOfNestedBlocks - absoluteBlockLevel
        assert(relativeLevel >= 0) { "Negative relative block index" }

        val fromTypeFlag = if (fromIsNullable) 0b01 else 0
        val toTypeFlag = if (toIsNullable) 0b10 else 0
        val flags = fromTypeFlag or toTypeFlag

        buildInstr(
            brOp,
            location,
            WasmImmediate.ConstU8(flags.toUByte()),
            WasmImmediate.LabelIdx(relativeLevel),
            WasmImmediate.HeapType(from),
            WasmImmediate.HeapType(to)
        )
    }

    fun buildBr(absoluteBlockLevel: Int, location: SourceLocation) {
        buildBrInstr(WasmOp.BR, absoluteBlockLevel, location)
    }

    fun buildThrow(tagIdx: WasmSymbol<Int>, location: SourceLocation) {
        buildInstr(WasmOp.THROW, location, WasmImmediate.TagIdx(tagIdx))
    }

    fun buildThrowRef(location: SourceLocation) {
        buildInstr(WasmOp.THROW_REF, location)
    }

    fun buildTry(resultType: WasmType? = null, body: (Int) -> Unit) {
        buildInstr(WasmOp.TRY, SourceLocation.NoLocation, WasmImmediate.BlockType.Value(resultType))
        body(numberOfNestedBlocks)
        buildEnd()
    }

    fun buildTryTable(
        catch1: WasmImmediate.Catch,
        catch2: WasmImmediate.Catch? = null,
        resultType: WasmType? = null,
        body: (Int) -> Unit
    ) {
        val catchSize = if (catch2 == null) 1 else 2

        if (catch2 == null) {
            buildInstr(
                WasmOp.TRY_TABLE,
                SourceLocation.NoLocation,
                WasmImmediate.BlockType.Value(resultType),
                WasmImmediate.ConstI32(catchSize),
                catch1,
            )
        } else {
            buildInstr(
                WasmOp.TRY_TABLE,
                SourceLocation.NoLocation,
                WasmImmediate.BlockType.Value(resultType),
                WasmImmediate.ConstI32(catchSize),
                catch1,
                catch2,
            )
        }
        body(numberOfNestedBlocks)
        buildEnd()
    }

    fun createNewCatch(tagIdx: WasmSymbol<Int>, absoluteBlockLevel: Int) =
        createNewCatchImmediate(WasmImmediate.Catch.CatchType.CATCH, absoluteBlockLevel, tagIdx)

    fun createNewCatchAll(absoluteBlockLevel: Int) =
        createNewCatchImmediate(WasmImmediate.Catch.CatchType.CATCH_ALL, absoluteBlockLevel)

    fun createNewCatchAllRef(absoluteBlockLevel: Int) =
        createNewCatchImmediate(WasmImmediate.Catch.CatchType.CATCH_ALL_REF, absoluteBlockLevel)

    private fun createNewCatchImmediate(
        catchType: WasmImmediate.Catch.CatchType,
        absoluteBlockLevel: Int,
        tagIdx: WasmSymbol<Int>? = null
    ): WasmImmediate.Catch {
        val relativeLevel = numberOfNestedBlocks - absoluteBlockLevel
        assert(relativeLevel >= 0) { "Negative relative block index" }

        return WasmImmediate.Catch(
            catchType,
            listOfNotNull(
                tagIdx?.let(WasmImmediate::TableIdx),
                WasmImmediate.LabelIdx(relativeLevel)
            )
        )
    }

    fun buildCatch(tagIdx: WasmSymbol<Int>, location: SourceLocation = SourceLocation.NoLocation("Catch")) {
        buildInstr(WasmOp.CATCH, location, WasmImmediate.TagIdx(tagIdx))
    }

    fun buildCatchAll() {
        buildInstr(WasmOp.CATCH_ALL, SourceLocation.NoLocation)
    }

    fun buildBrIf(absoluteBlockLevel: Int, location: SourceLocation) {
        buildBrInstr(WasmOp.BR_IF, absoluteBlockLevel, location)
    }

    fun buildCall(symbol: WasmSymbol<WasmFunction>, location: SourceLocation) {
        buildInstr(WasmOp.CALL, location, WasmImmediate.FuncIdx(symbol))
    }

    fun buildCallIndirect(
        symbol: WasmSymbol<WasmFunctionType>,
        tableIdx: WasmSymbolReadOnly<Int> = WasmSymbol(0),
        location: SourceLocation
    ) {
        buildInstr(
            WasmOp.CALL_INDIRECT,
            location,
            WasmImmediate.TypeIdx(symbol),
            WasmImmediate.TableIdx(tableIdx)
        )
    }

    fun buildGetLocal(local: WasmLocal, location: SourceLocation) {
        buildInstr(WasmOp.LOCAL_GET, location, WasmImmediate.LocalIdx(local))
    }

    fun buildSetLocal(local: WasmLocal, location: SourceLocation) {
        buildInstr(WasmOp.LOCAL_SET, location, WasmImmediate.LocalIdx(local))
    }

    fun buildTeeLocal(local: WasmLocal, location: SourceLocation) {
        buildInstr(WasmOp.LOCAL_TEE, location, WasmImmediate.LocalIdx(local))
    }

    fun buildGetGlobal(global: WasmSymbol<WasmGlobal>, location: SourceLocation) {
        buildInstr(WasmOp.GLOBAL_GET, location, WasmImmediate.GlobalIdx(global))
    }

    fun buildSetGlobal(global: WasmSymbol<WasmGlobal>, location: SourceLocation) {
        buildInstr(WasmOp.GLOBAL_SET, location, WasmImmediate.GlobalIdx(global))
    }

    fun buildStructGet(struct: WasmSymbol<WasmTypeDeclaration>, fieldId: Int, location: SourceLocation) {
        buildInstr(
            WasmOp.STRUCT_GET,
            location,
            WasmImmediate.GcType(struct),
            WasmImmediate.StructFieldIdx(fieldId)
        )
    }

    fun buildStructNew(struct: WasmSymbol<WasmTypeDeclaration>, location: SourceLocation) {
        buildInstr(WasmOp.STRUCT_NEW, location, WasmImmediate.GcType(struct))
    }

    fun buildStructSet(struct: WasmSymbol<WasmTypeDeclaration>, fieldId: Int, location: SourceLocation) {
        buildInstr(
            WasmOp.STRUCT_SET,
            location,
            WasmImmediate.GcType(struct),
            WasmImmediate.StructFieldIdx(fieldId)
        )
    }

    fun buildRefCastNullStatic(toType: WasmSymbolReadOnly<WasmTypeDeclaration>, location: SourceLocation) {
        buildInstr(WasmOp.REF_CAST_NULL, location, WasmImmediate.HeapType(WasmHeapType.Type(toType)))
    }

    fun buildRefCastStatic(toType: WasmSymbolReadOnly<WasmTypeDeclaration>, location: SourceLocation) {
        buildInstr(WasmOp.REF_CAST, location, WasmImmediate.HeapType(WasmHeapType.Type(toType)))
    }

    fun buildRefTestStatic(toType: WasmSymbolReadOnly<WasmTypeDeclaration>, location: SourceLocation) {
        buildInstr(WasmOp.REF_TEST, location, WasmImmediate.HeapType(WasmHeapType.Type(toType)))
    }

    fun buildRefNull(type: WasmHeapType, location: SourceLocation) {
        buildInstr(WasmOp.REF_NULL, location, WasmImmediate.HeapType(WasmRefType(type)))
    }

    fun buildDrop(location: SourceLocation) {
        buildInstr(WasmOp.DROP, location)
    }

    fun buildNop(location: SourceLocation) {
        buildInstr(WasmOp.NOP, location)
    }

    inline fun commentPreviousInstr(text: () -> String) {
        if (!skipCommentInstructions) {
            buildInstr(
                WasmOp.PSEUDO_COMMENT_PREVIOUS_INSTR,
                SourceLocation.NoLocation("Pseudo-instruction"),
                WasmImmediate.ConstString(text())
            )
        }
    }

    inline fun commentGroupStart(text: () -> String) {
        if (!skipCommentInstructions) {
            buildInstr(WasmOp.PSEUDO_COMMENT_GROUP_START, SourceLocation.NoLocation("Pseudo-instruction"), WasmImmediate.ConstString(text()))
        }
    }

    fun commentGroupEnd() {
        if (!skipCommentInstructions) {
            buildInstr(WasmOp.PSEUDO_COMMENT_GROUP_END, SourceLocation.NoLocation("Pseudo-instruction"))
        }
    }
}

inline fun buildWasmExpression(body: WasmExpressionBuilder.() -> Unit): MutableList<WasmInstr> {
    val res = mutableListOf<WasmInstr>()
    WasmExpressionBuilder(res).body()
    return res
}