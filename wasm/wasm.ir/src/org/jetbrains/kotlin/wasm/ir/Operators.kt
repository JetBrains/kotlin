/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import org.jetbrains.kotlin.wasm.ir.WasmImmediateKind.*
import java.util.EnumSet

enum class WasmImmediateKind {
    CONST_U8,
    CONST_I32,
    CONST_I64,
    CONST_F32,
    CONST_F64,

    MEM_ARG,

    BLOCK_TYPE,
    LOCAL_DEFS,

    FUNC_IDX,
    LOCAL_IDX,
    GLOBAL_IDX,
    TYPE_IDX,
    VAL_TYPE_VECTOR,
    MEMORY_IDX,
    DATA_IDX,
    TABLE_IDX,
    LABEL_IDX,
    TAG_IDX,
    LABEL_IDX_VECTOR,
    ELEM_IDX,

    STRUCT_TYPE_IDX,
    STRUCT_FIELD_IDX,
    TYPE_IMM,
    HEAP_TYPE,

    CATCH_VECTOR
}

sealed class WasmImmediate {
    class ConstU8(val value: UByte) : WasmImmediate()
    class ConstI32(val value: Int) : WasmImmediate()
    class ConstI64(val value: Long) : WasmImmediate()
    class ConstF32(val rawBits: UInt) : WasmImmediate()
    class ConstF64(val rawBits: ULong) : WasmImmediate()
    class SymbolI32(val value: WasmSymbol<Int>) : WasmImmediate()

    class MemArg(val align: UInt, val offset: UInt) : WasmImmediate()

    sealed class BlockType : WasmImmediate() {
        class Function(val type: WasmFunctionType) : BlockType()
        class Value(val type: WasmType?) : BlockType()
    }

    class FuncIdx(val value: WasmSymbol<WasmFunction>) : WasmImmediate() {
        constructor(value: WasmFunction) : this(WasmSymbol(value))
    }

    class LocalIdx(val value: WasmSymbol<WasmLocal>) : WasmImmediate() {
        constructor(value: WasmLocal) : this(WasmSymbol(value))
    }

    class GlobalIdx(val value: WasmSymbol<WasmGlobal>) : WasmImmediate() {
        constructor(value: WasmGlobal) : this(WasmSymbol(value))
    }

    class TypeIdx(val value: WasmSymbolReadOnly<WasmTypeDeclaration>) : WasmImmediate() {
        constructor(value: WasmTypeDeclaration) : this(WasmSymbol(value))
    }

    class ValTypeVector(val value: List<WasmType>) : WasmImmediate()

    class MemoryIdx(val value: Int) : WasmImmediate()

    class DataIdx(val value: WasmSymbol<Int>) : WasmImmediate() {
        constructor(value: Int) : this(WasmSymbol(value))
    }

    class TableIdx(val value: WasmSymbolReadOnly<Int>) : WasmImmediate() {
        constructor(value: Int) : this(WasmSymbol(value))
    }

    class LabelIdx(val value: Int) : WasmImmediate()
    class TagIdx(val value: Int) : WasmImmediate()
    class LabelIdxVector(val value: List<Int>) : WasmImmediate()
    class ElemIdx(val value: WasmElement) : WasmImmediate()

    class GcType(val value: WasmSymbol<WasmTypeDeclaration>) : WasmImmediate() {
        constructor(value: WasmTypeDeclaration) : this(WasmSymbol(value))
    }

    class StructFieldIdx(val value: WasmSymbol<Int>) : WasmImmediate()

    class HeapType(val value: WasmHeapType) : WasmImmediate() {
        constructor(type: WasmType) : this(type.getHeapType())
    }

    class Catch(val type: CatchType, val immediates: List<WasmImmediate>) : WasmImmediate() {
        init {
            require(immediates.size == type.immediates.size) { "Immediates sizes are not equals: ${type.name} required ${type.immediates.size}, but ${immediates.size} were provided" }
        }

        enum class CatchType(val mnemonic: String, val opcode: Int, vararg val immediates: WasmImmediateKind) {
            CATCH("catch", 0x00, TAG_IDX, LABEL_IDX),
            CATCH_REF("catch_ref", 0x01, TAG_IDX, LABEL_IDX),
            CATCH_ALL("catch_all", 0x02, LABEL_IDX),
            CATCH_ALL_REF("catch_all_ref", 0x03, LABEL_IDX)
        }
    }

    // Pseudo-immediates
    class ConstString(val value: String) : WasmImmediate()
}


enum class WasmOp(
    val mnemonic: String,
    val opcode: Int,
    val immediates: List<WasmImmediateKind> = emptyList()
) {

    // Unary
    I32_EQZ("i32.eqz", 0x45),
    I64_EQZ("i64.eqz", 0x50),
    I32_CLZ("i32.clz", 0x67),
    I32_CTZ("i32.ctz", 0x68),
    I32_POPCNT("i32.popcnt", 0x69),
    I64_CLZ("i64.clz", 0x79),
    I64_CTZ("i64.ctz", 0x7A),
    I64_POPCNT("i64.popcnt", 0x7B),
    F32_ABS("f32.abs", 0x8B),
    F32_NEG("f32.neg", 0x8C),
    F32_CEIL("f32.ceil", 0x8D),
    F32_FLOOR("f32.floor", 0x8E),
    F32_TRUNC("f32.trunc", 0x8F),
    F32_NEAREST("f32.nearest", 0x90),
    F32_SQRT("f32.sqrt", 0x91),
    F64_ABS("f64.abs", 0x99),
    F64_NEG("f64.neg", 0x9A),
    F64_CEIL("f64.ceil", 0x9B),
    F64_FLOOR("f64.floor", 0x9C),
    F64_TRUNC("f64.trunc", 0x9D),
    F64_NEAREST("f64.nearest", 0x9E),
    F64_SQRT("f64.sqrt", 0x9F),
    I32_WRAP_I64("i32.wrap_i64", 0xA7),
    I32_TRUNC_F32_S("i32.trunc_f32_s", 0xA8),
    I32_TRUNC_F32_U("i32.trunc_f32_u", 0xA9),
    I32_TRUNC_F64_S("i32.trunc_f64_s", 0xAA),
    I32_TRUNC_F64_U("i32.trunc_f64_u", 0xAB),
    I64_EXTEND_I32_S("i64.extend_i32_s", 0xAC),
    I64_EXTEND_I32_U("i64.extend_i32_u", 0xAD),
    I64_TRUNC_F32_S("i64.trunc_f32_s", 0xAE),
    I64_TRUNC_F32_U("i64.trunc_f32_u", 0xAF),
    I64_TRUNC_F64_S("i64.trunc_f64_s", 0xB0),
    I64_TRUNC_F64_U("i64.trunc_f64_u", 0xB1),
    F32_CONVERT_I32_S("f32.convert_i32_s", 0xB2),
    F32_CONVERT_I32_U("f32.convert_i32_u", 0xB3),
    F32_CONVERT_I64_S("f32.convert_i64_s", 0xB4),
    F32_CONVERT_I64_U("f32.convert_i64_u", 0xB5),
    F32_DEMOTE_F64("f32.demote_f64", 0xB6),
    F64_CONVERT_I32_S("f64.convert_i32_s", 0xB7),
    F64_CONVERT_I32_U("f64.convert_i32_u", 0xB8),
    F64_CONVERT_I64_S("f64.convert_i64_s", 0xB9),
    F64_CONVERT_I64_U("f64.convert_i64_u", 0xBA),
    F64_PROMOTE_F32("f64.promote_f32", 0xBB),
    I32_REINTERPRET_F32("i32.reinterpret_f32", 0xBC),
    I64_REINTERPRET_F64("i64.reinterpret_f64", 0xBD),
    F32_REINTERPRET_I32("f32.reinterpret_i32", 0xBE),
    F64_REINTERPRET_I64("f64.reinterpret_i64", 0xBF),
    I32_EXTEND8_S("i32.extend8_s", 0xC0),
    I32_EXTEND16_S("i32.extend16_s", 0xC1),
    I64_EXTEND8_S("i64.extend8_s", 0xC2),
    I64_EXTEND16_S("i64.extend16_s", 0xC3),
    I64_EXTEND32_S("i64.extend32_s", 0xC4),

    // Non-trapping float to int
    I32_TRUNC_SAT_F32_S("i32.trunc_sat_f32_s", 0xFC_00),
    I32_TRUNC_SAT_F32_U("i32.trunc_sat_f32_u", 0xFC_01),
    I32_TRUNC_SAT_F64_S("i32.trunc_sat_f64_s", 0xFC_02),
    I32_TRUNC_SAT_F64_U("i32.trunc_sat_f64_u", 0xFC_03),
    I64_TRUNC_SAT_F32_S("i64.trunc_sat_f32_s", 0xFC_04),
    I64_TRUNC_SAT_F32_U("i64.trunc_sat_f32_u", 0xFC_05),
    I64_TRUNC_SAT_F64_S("i64.trunc_sat_f64_s", 0xFC_06),
    I64_TRUNC_SAT_F64_U("i64.trunc_sat_f64_u", 0xFC_07),

    // Binary
    I32_EQ("i32.eq", 0x46),
    I32_NE("i32.ne", 0x47),
    I32_LT_S("i32.lt_s", 0x48),
    I32_LT_U("i32.lt_u", 0x49),
    I32_GT_S("i32.gt_s", 0x4A),
    I32_GT_U("i32.gt_u", 0x4B),
    I32_LE_S("i32.le_s", 0x4C),
    I32_LE_U("i32.le_u", 0x4D),
    I32_GE_S("i32.ge_s", 0x4E),
    I32_GE_U("i32.ge_u", 0x4F),
    I64_EQ("i64.eq", 0x51),
    I64_NE("i64.ne", 0x52),
    I64_LT_S("i64.lt_s", 0x53),
    I64_LT_U("i64.lt_u", 0x54),
    I64_GT_S("i64.gt_s", 0x55),
    I64_GT_U("i64.gt_u", 0x56),
    I64_LE_S("i64.le_s", 0x57),
    I64_LE_U("i64.le_u", 0x58),
    I64_GE_S("i64.ge_s", 0x59),
    I64_GE_U("i64.ge_u", 0x5A),
    F32_EQ("f32.eq", 0x5B),
    F32_NE("f32.ne", 0x5C),
    F32_LT("f32.lt", 0x5D),
    F32_GT("f32.gt", 0x5E),
    F32_LE("f32.le", 0x5F),
    F32_GE("f32.ge", 0x60),
    F64_EQ("f64.eq", 0x61),
    F64_NE("f64.ne", 0x62),
    F64_LT("f64.lt", 0x63),
    F64_GT("f64.gt", 0x64),
    F64_LE("f64.le", 0x65),
    F64_GE("f64.ge", 0x66),
    I32_ADD("i32.add", 0x6A),
    I32_SUB("i32.sub", 0x6B),
    I32_MUL("i32.mul", 0x6C),
    I32_DIV_S("i32.div_s", 0x6D),
    I32_DIV_U("i32.div_u", 0x6E),
    I32_REM_S("i32.rem_s", 0x6F),
    I32_REM_U("i32.rem_u", 0x70),
    I32_AND("i32.and", 0x71),
    I32_OR("i32.or", 0x72),
    I32_XOR("i32.xor", 0x73),
    I32_SHL("i32.shl", 0x74),
    I32_SHR_S("i32.shr_s", 0x75),
    I32_SHR_U("i32.shr_u", 0x76),
    I32_ROTL("i32.rotl", 0x77),
    I32_ROTR("i32.rotr", 0x78),
    I64_ADD("i64.add", 0x7C),
    I64_SUB("i64.sub", 0x7D),
    I64_MUL("i64.mul", 0x7E),
    I64_DIV_S("i64.div_s", 0x7F),
    I64_DIV_U("i64.div_u", 0x80),
    I64_REM_S("i64.rem_s", 0x81),
    I64_REM_U("i64.rem_u", 0x82),
    I64_AND("i64.and", 0x83),
    I64_OR("i64.or", 0x84),
    I64_XOR("i64.xor", 0x85),
    I64_SHL("i64.shl", 0x86),
    I64_SHR_S("i64.shr_s", 0x87),
    I64_SHR_U("i64.shr_u", 0x88),
    I64_ROTL("i64.rotl", 0x89),
    I64_ROTR("i64.rotr", 0x8A),
    F32_ADD("f32.add", 0x92),
    F32_SUB("f32.sub", 0x93),
    F32_MUL("f32.mul", 0x94),
    F32_DIV("f32.div", 0x95),
    F32_MIN("f32.min", 0x96),
    F32_MAX("f32.max", 0x97),
    F32_COPYSIGN("f32.copysign", 0x98),
    F64_ADD("f64.add", 0xA0),
    F64_SUB("f64.sub", 0xA1),
    F64_MUL("f64.mul", 0xA2),
    F64_DIV("f64.div", 0xA3),
    F64_MIN("f64.min", 0xA4),
    F64_MAX("f64.max", 0xA5),
    F64_COPYSIGN("f64.copysign", 0xA6),

    // Constants
    I32_CONST("i32.const", 0x41, CONST_I32),
    I64_CONST("i64.const", 0x42, CONST_I64),
    F32_CONST("f32.const", 0x43, CONST_F32),
    F64_CONST("f64.const", 0x44, CONST_F64),

    // Load
    I32_LOAD("i32.load", 0x28, MEM_ARG),
    I64_LOAD("i64.load", 0x29, MEM_ARG),
    F32_LOAD("f32.load", 0x2A, MEM_ARG),
    F64_LOAD("f64.load", 0x2B, MEM_ARG),
    I32_LOAD8_S("i32.load8_s", 0x2C, MEM_ARG),
    I32_LOAD8_U("i32.load8_u", 0x2D, MEM_ARG),
    I32_LOAD16_S("i32.load16_s", 0x2E, MEM_ARG),
    I32_LOAD16_U("i32.load16_u", 0x2F, MEM_ARG),
    I64_LOAD8_S("i64.load8_s", 0x30, MEM_ARG),
    I64_LOAD8_U("i64.load8_u", 0x31, MEM_ARG),
    I64_LOAD16_S("i64.load16_s", 0x32, MEM_ARG),
    I64_LOAD16_U("i64.load16_u", 0x33, MEM_ARG),
    I64_LOAD32_S("i64.load32_s", 0x34, MEM_ARG),
    I64_LOAD32_U("i64.load32_u", 0x35, MEM_ARG),

    // Store
    I32_STORE("i32.store", 0x36, MEM_ARG),
    I64_STORE("i64.store", 0x37, MEM_ARG),
    F32_STORE("f32.store", 0x38, MEM_ARG),
    F64_STORE("f64.store", 0x39, MEM_ARG),
    I32_STORE8("i32.store8", 0x3A, MEM_ARG),
    I32_STORE16("i32.store16", 0x3B, MEM_ARG),
    I64_STORE8("i64.store8", 0x3C, MEM_ARG),
    I64_STORE16("i64.store16", 0x3D, MEM_ARG),
    I64_STORE32("i64.store32", 0x3E, MEM_ARG),

    // Memory
    MEMORY_SIZE("memory.size", 0x3F, MEMORY_IDX),
    MEMORY_GROW("memory.grow", 0x40, MEMORY_IDX),
    MEMORY_INIT("memory.init", 0xFC_08, listOf(DATA_IDX, MEMORY_IDX)),
    DATA_DROP("data.drop", 0xFC_09, DATA_IDX),
    MEMORY_COPY("memory.copy", 0xFC_0A, listOf(MEMORY_IDX, MEMORY_IDX)),
    MEMORY_FILL("memory.fill", 0xFC_0B, MEMORY_IDX),

    // Table
    TABLE_GET("table.get", 0x25, TABLE_IDX),
    TABLE_SET("table.set", 0x26, TABLE_IDX),
    TABLE_GROW("table.grow", 0xFC_0F, TABLE_IDX),
    TABLE_SIZE("table.size", 0xFC_10, TABLE_IDX),
    TABLE_FILL("table.fill", 0xFC_11, TABLE_IDX),
    TABLE_INIT("table.init", 0xFC_0C, listOf(ELEM_IDX, TABLE_IDX)),
    ELEM_DROP("elem.drop", 0xFC_0D, ELEM_IDX),
    TABLE_COPY("table.copy", 0xFC_0E, listOf(TABLE_IDX, TABLE_IDX)),

    // Control
    UNREACHABLE("unreachable", 0x00),
    NOP("nop", 0x01),
    BLOCK("block", 0x02, BLOCK_TYPE),
    LOOP("loop", 0x03, BLOCK_TYPE),
    IF("if", 0x04, BLOCK_TYPE),
    ELSE("else", 0x05),
    END("end", 0x0B),
    BR("br", 0x0C, LABEL_IDX),
    BR_IF("br_if", 0x0D, LABEL_IDX),
    BR_TABLE("br_table", 0x0E, listOf(LABEL_IDX_VECTOR, LABEL_IDX)),
    RETURN("return", 0x0F),
    CALL("call", 0x10, FUNC_IDX),
    CALL_INDIRECT("call_indirect", 0x11, listOf(TYPE_IDX, TABLE_IDX)),
    TRY("try", 0x06, BLOCK_TYPE),
    CATCH("catch", 0x07, TAG_IDX),
    CATCH_ALL("catch_all", 0x19),
    DELEGATE("delegate", 0x18, LABEL_IDX),
    THROW("throw", 0x08, TAG_IDX),
    RETHROW("rethrow", 0x09, LABEL_IDX),

    // Parametric
    DROP("drop", 0x1A),
    SELECT("select", 0x1B),
    SELECT_TYPED("select", 0x1C, VAL_TYPE_VECTOR),

    // Variable OP
    LOCAL_GET("local.get", 0x20, LOCAL_IDX),
    LOCAL_SET("local.set", 0x21, LOCAL_IDX),
    LOCAL_TEE("local.tee", 0x22, LOCAL_IDX),
    GLOBAL_GET("global.get", 0x23, GLOBAL_IDX),
    GLOBAL_SET("global.set", 0x24, GLOBAL_IDX),

    // Reference types
    REF_NULL("ref.null", 0xD0, HEAP_TYPE),
    REF_IS_NULL("ref.is_null", 0xD1),
    REF_FUNC("ref.func", 0xD2, FUNC_IDX),

    // ============================================================
    // Typed Function References
    // WIP: https://github.com/WebAssembly/function-references
    CALL_REF("call_ref", 0x14, TYPE_IDX),
    RETURN_CALL_REF("return_call_ref", 0x15, TYPE_IDX),
    REF_AS_NOT_NULL("ref.as_non_null", 0xD4),
    BR_ON_NULL("br_on_null", 0xD5, LABEL_IDX),
    BR_ON_NON_NULL("br_on_non_null", 0xD6, LABEL_IDX),


    // ============================================================
    // GC
    // WIP: https://github.com/WebAssembly/gc
    STRUCT_NEW("struct.new", 0xFB_00, STRUCT_TYPE_IDX),
    STRUCT_NEW_DEFAULT("struct.new_default", 0xFB_01, STRUCT_TYPE_IDX),
    STRUCT_GET("struct.get", 0xFB_02, listOf(STRUCT_TYPE_IDX, STRUCT_FIELD_IDX)),
    STRUCT_GET_S("struct.get_s", 0xFB_03, listOf(STRUCT_TYPE_IDX, STRUCT_FIELD_IDX)),
    STRUCT_GET_U("struct.get_u", 0xFB_04, listOf(STRUCT_TYPE_IDX, STRUCT_FIELD_IDX)),
    STRUCT_SET("struct.set", 0xFB_05, listOf(STRUCT_TYPE_IDX, STRUCT_FIELD_IDX)),

    ARRAY_NEW("array.new", 0xFB_06, STRUCT_TYPE_IDX),
    ARRAY_NEW_DEFAULT("array.new_default", 0xFB_07, STRUCT_TYPE_IDX),
    ARRAY_GET("array.get", 0xFB_0B, listOf(STRUCT_TYPE_IDX)),
    ARRAY_GET_S("array.get_s", 0xFB_0C, listOf(STRUCT_TYPE_IDX)),
    ARRAY_GET_U("array.get_u", 0xFB_0D, listOf(STRUCT_TYPE_IDX)),
    ARRAY_SET("array.set", 0xFB_0E, listOf(STRUCT_TYPE_IDX)),
    ARRAY_LEN("array.len", 0xFB_0F),
    // ARRAY_FILL,
    ARRAY_COPY("array.copy", 0xFB_11, listOf(STRUCT_TYPE_IDX, STRUCT_TYPE_IDX)),
    ARRAY_NEW_DATA("array.new_data", 0xFB_09, listOf(STRUCT_TYPE_IDX, DATA_IDX)),
    ARRAY_NEW_FIXED("array.new_fixed", 0xFB_08, listOf(STRUCT_TYPE_IDX, CONST_I32)),
//    ARRAY_NEW_ELEM("array.new_elem", 0xFB_0A, listOf(STRUCT_TYPE_IDX, ELEM_IDX)),

    I31_NEW("i31.new", 0xFB_1C),
    I31_GET_S("i31.get_s", 0xFB_1D),
    I31_GET_U("i31.get_u", 0xFB_1E),

    REF_EQ("ref.eq", 0xD3),
    REF_TEST("ref.test", 0xFB_14, HEAP_TYPE),
    REF_TEST_NULL("ref.test null", 0xFB_15, HEAP_TYPE),
    REF_CAST("ref.cast", 0xFB_16, HEAP_TYPE),
    REF_CAST_NULL("ref.cast null", 0xFB_17, HEAP_TYPE),

    BR_ON_CAST("br_on_cast", 0xFB_18, listOf(CONST_U8, LABEL_IDX, HEAP_TYPE, HEAP_TYPE)),
    BR_ON_CAST_FAIL("br_on_cast_fail", 0xFB_19, listOf(CONST_U8, LABEL_IDX, HEAP_TYPE, HEAP_TYPE)),

    EXTERN_INTERNALIZE("extern.internalize", 0xFB_1A), // externref -> anyref
    EXTERN_EXTERNALIZE("extern.externalize", 0xFB_1B), // anyref -> externref

    // ============================================================
    // Exception handling
    // WIP: https://github.com/WebAssembly/exception-handling
    TRY_TABLE("try_table", 0x1f, listOf(BLOCK_TYPE, CONST_I32, CATCH_VECTOR)),
    THROW_REF("throw_ref", 0x0a, LABEL_IDX),

    // ============================================================
    PSEUDO_COMMENT_PREVIOUS_INSTR("<comment-single>", WASM_OP_PSEUDO_OPCODE),
    PSEUDO_COMMENT_GROUP_START("<comment-group-start>", WASM_OP_PSEUDO_OPCODE),
    PSEUDO_COMMENT_GROUP_END("<comment-group-end>", WASM_OP_PSEUDO_OPCODE),

    // Macro commands needed to optionally emit code (needed to support IC for ITables
    MACRO_IF("<macro-if>", WASM_OP_PSEUDO_OPCODE, listOf(CONST_I32)),
    MACRO_ELSE("<macro-else>", WASM_OP_PSEUDO_OPCODE),
    MACRO_END_IF("<macro-end-if>", WASM_OP_PSEUDO_OPCODE),
    // Macro IF emits `then` part only if immediate linked to 1 else emits ELSE part
    // MACRO_IF
    // instructions1...
    // MACRO_ELSE
    // instructions2...
    // MACRO_END

    MACRO_TABLE("<macro-table>", WASM_OP_PSEUDO_OPCODE, listOf(CONST_I32)),
    MACRO_TABLE_INDEX("<macro-table-index>", WASM_OP_PSEUDO_OPCODE, listOf(CONST_I32)),
    MACRO_TABLE_END("<macro-table-end>", WASM_OP_PSEUDO_OPCODE),
    // Macro table emits instructions spreaded with nullrefs, like:
    // MACRO_TABLE TableSize
    // MACRO_TABLE_INDEX 2
    // instructions1...
    // MACRO_TABLE_INDEX 4
    // instructions2...
    // MACRO_TABLE_END
    // emits:
    // nullref
    // nullref
    // instructions1
    // nullref
    // instructions2
    ;

    constructor(mnemonic: String, opcode: Int, vararg immediates: WasmImmediateKind) : this(mnemonic, opcode, immediates.toList())
}

const val WASM_OP_PSEUDO_OPCODE = 0xFFFF

val opcodesToOp: Map<Int, WasmOp> =
    enumValues<WasmOp>().associateBy { it.opcode }