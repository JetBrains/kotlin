/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

interface WasmOp {
    val mnemonic: String
    val opcode: Int
}

interface WasmTypedOp : WasmOp {
    val type: WasmValueType?

    val operandsCount: Int
    fun getOperandType(index: Int): WasmValueType
}

enum class WasmUnaryOp(
    override val mnemonic: String,
    override val opcode: Int,
    val operandType: WasmValueType,
    override val type: WasmValueType,
) : WasmTypedOp {
    I32_EQZ("i32.eqz", 0x45, WasmI1, WasmI1),
    I64_EQZ("i64.eqz", 0x50, WasmI32, WasmI64),

    I32_CLZ("i32.clz", 0x67, WasmI32, WasmI32),
    I32_CTZ("i32.ctz", 0x68, WasmI32, WasmI32),
    I32_POPCNT("i32.popcnt", 0x69, WasmI32, WasmI32),

    I64_CLZ("i64.clz", 0x79, WasmI64, WasmI64),
    I64_CTZ("i64.ctz", 0x7A, WasmI64, WasmI64),
    I64_POPCNT("i64.popcnt", 0x7B, WasmI64, WasmI64),

    F32_ABS("f32.abs", 0x8B, WasmF32, WasmF32),
    F32_NEG("f32.neg", 0x8C, WasmF32, WasmF32),
    F32_CEIL("f32.ceil", 0x8D, WasmF32, WasmF32),
    F32_FLOOR("f32.floor", 0x8E, WasmF32, WasmF32),
    F32_TRUNC("f32.trunc", 0x8F, WasmF32, WasmF32),
    F32_NEAREST("f32.nearest", 0x90, WasmF32, WasmF32),
    F32_SQRT("f32.sqrt", 0x91, WasmF32, WasmF32),

    F64_ABS("f64.abs", 0x99, WasmF64, WasmF64),
    F64_NEG("f64.neg", 0x9A, WasmF64, WasmF64),
    F64_CEIL("f64.ceil", 0x9B, WasmF64, WasmF64),
    F64_FLOOR("f64.floor", 0x9C, WasmF64, WasmF64),
    F64_TRUNC("f64.trunc", 0x9D, WasmF64, WasmF64),
    F64_NEAREST("f64.nearest", 0x9E, WasmF64, WasmF64),
    F64_SQRT("f64.sqrt", 0x9F, WasmF64, WasmF64),

    I32_WRAP_I64("i32.wrap_i64", 0xA7, WasmI64, WasmI32),
    I32_TRUNC_F32_S("i32.trunc_f32_s", 0xA8, WasmF32, WasmI32),
    I32_TRUNC_F32_U("i32.trunc_f32_u", 0xA9, WasmF32, WasmI32),
    I32_TRUNC_F64_S("i32.trunc_f64_s", 0xAA, WasmF64, WasmI32),
    I32_TRUNC_F64_U("i32.trunc_f64_u", 0xAB, WasmF64, WasmI32),
    I64_EXTEND_I32_S("i64.extend_i32_s", 0xAC, WasmI32, WasmI64),
    I64_EXTEND_I32_U("i64.extend_i32_u", 0xAD, WasmI32, WasmI64),
    I64_TRUNC_F32_S("i64.trunc_f32_s", 0xAE, WasmF32, WasmI64),
    I64_TRUNC_F32_U("i64.trunc_f32_u", 0xAF, WasmF32, WasmI64),
    I64_TRUNC_F64_S("i64.trunc_f64_s", 0xB0, WasmF64, WasmI64),
    I64_TRUNC_F64_U("i64.trunc_f64_u", 0xB1, WasmF64, WasmI64),
    F32_CONVERT_I32_S("f32.convert_i32_s", 0xB2, WasmI32, WasmF32),
    F32_CONVERT_I32_U("f32.convert_i32_u", 0xB3, WasmI32, WasmF32),
    F32_CONVERT_I64_S("f32.convert_i64_s", 0xB4, WasmI64, WasmF32),
    F32_CONVERT_I64_U("f32.convert_i64_u", 0xB5, WasmI64, WasmF32),
    F32_DEMOTE_F64("f32.demote_f64", 0xB6, WasmF64, WasmF32),
    F64_CONVERT_I32_S("f64.convert_i32_s", 0xB7, WasmI32, WasmF64),
    F64_CONVERT_I32_U("f64.convert_i32_u", 0xB8, WasmI32, WasmF64),
    F64_CONVERT_I64_S("f64.convert_i64_s", 0xB9, WasmI64, WasmF64),
    F64_CONVERT_I64_U("f64.convert_i64_u", 0xBA, WasmI64, WasmF64),
    F64_PROMOTE_F32("f64.promote_f32", 0xBB, WasmF32, WasmF64),
    I32_REINTERPRET_F32("i32.reinterpret_f32", 0xBC, WasmF32, WasmI32),
    I64_REINTERPRET_F64("i64.reinterpret_f64", 0xBD, WasmF64, WasmI64),
    F32_REINTERPRET_I32("f32.reinterpret_i32", 0xBE, WasmI32, WasmF32),
    F64_REINTERPRET_I64("f64.reinterpret_i64", 0xBF, WasmI64, WasmF64),

    // Non-trapping float to int
    I32_TRUNC_SAT_F32_S("i32.trunc_sat_f32_s", 0xFC_00, WasmF32, WasmI32),
    I32_TRUNC_SAT_F32_U("i32.trunc_sat_f32_u", 0xFC_01, WasmF32, WasmI32),
    I32_TRUNC_SAT_F64_S("i32.trunc_sat_f64_s", 0xFC_02, WasmF64, WasmI32),
    I32_TRUNC_SAT_F64_U("i32.trunc_sat_f64_u", 0xFC_03, WasmF64, WasmI32),
    I64_TRUNC_SAT_F32_S("i64.trunc_sat_f32_s", 0xFC_04, WasmF32, WasmI64),
    I64_TRUNC_SAT_F32_U("i64.trunc_sat_f32_u", 0xFC_05, WasmF32, WasmI64),
    I64_TRUNC_SAT_F64_S("i64.trunc_sat_f64_s", 0xFC_06, WasmF64, WasmI64),
    I64_TRUNC_SAT_F64_U("i64.trunc_sat_f64_u", 0xFC_07, WasmF64, WasmI64);

    override val operandsCount: Int = 1
    override fun getOperandType(index: Int): WasmValueType {
        require(index == 0)
        return operandType
    }
}

enum class WasmBinaryOp(
    override val mnemonic: String,
    override val opcode: Int,
    val lhsType: WasmValueType,
    val rhsType: WasmValueType,
    override val type: WasmValueType
) : WasmTypedOp {
    I32_EQ("i32.eq", 0x46, WasmI32, WasmI32, WasmI1),
    I32_NE("i32.ne", 0x47, WasmI32, WasmI32, WasmI1),
    I32_LT_S("i32.lt_s", 0x48, WasmI32, WasmI32, WasmI1),
    I32_LT_U("i32.lt_u", 0x49, WasmI32, WasmI32, WasmI1),
    I32_GT_S("i32.gt_s", 0x4A, WasmI32, WasmI32, WasmI1),
    I32_GT_U("i32.gt_u", 0x4B, WasmI32, WasmI32, WasmI1),
    I32_LE_S("i32.le_s", 0x4C, WasmI32, WasmI32, WasmI1),
    I32_LE_U("i32.le_u", 0x4D, WasmI32, WasmI32, WasmI1),
    I32_GE_S("i32.ge_s", 0x4E, WasmI32, WasmI32, WasmI1),
    I32_GE_U("i32.ge_u", 0x4F, WasmI32, WasmI32, WasmI1),
    I64_EQ("i64.eq", 0x51, WasmI64, WasmI64, WasmI1),
    I64_NE("i64.ne", 0x52, WasmI64, WasmI64, WasmI1),
    I64_LT_S("i64.lt_s", 0x53, WasmI64, WasmI64, WasmI1),
    I64_LT_U("i64.lt_u", 0x54, WasmI64, WasmI64, WasmI1),
    I64_GT_S("i64.gt_s", 0x55, WasmI64, WasmI64, WasmI1),
    I64_GT_U("i64.gt_u", 0x56, WasmI64, WasmI64, WasmI1),
    I64_LE_S("i64.le_s", 0x57, WasmI64, WasmI64, WasmI1),
    I64_LE_U("i64.le_u", 0x58, WasmI64, WasmI64, WasmI1),
    I64_GE_S("i64.ge_s", 0x59, WasmI64, WasmI64, WasmI1),
    I64_GE_U("i64.ge_u", 0x5A, WasmI64, WasmI64, WasmI1),
    F32_EQ("f32.eq", 0x5B, WasmF32, WasmF32, WasmI1),
    F32_NE("f32.ne", 0x5C, WasmF32, WasmF32, WasmI1),
    F32_LT("f32.lt", 0x5D, WasmF32, WasmF32, WasmI1),
    F32_GT("f32.gt", 0x5E, WasmF32, WasmF32, WasmI1),
    F32_LE("f32.le", 0x5F, WasmF32, WasmF32, WasmI1),
    F32_GE("f32.ge", 0x60, WasmF32, WasmF32, WasmI1),
    F64_EQ("f64.eq", 0x61, WasmF64, WasmF64, WasmI1),
    F64_NE("f64.ne", 0x62, WasmF64, WasmF64, WasmI1),
    F64_LT("f64.lt", 0x63, WasmF64, WasmF64, WasmI1),
    F64_GT("f64.gt", 0x64, WasmF64, WasmF64, WasmI1),
    F64_LE("f64.le", 0x65, WasmF64, WasmF64, WasmI1),
    F64_GE("f64.ge", 0x66, WasmF64, WasmF64, WasmI1),
    I32_ADD("i32.add", 0x6A, WasmI32, WasmI32, WasmI32),
    I32_SUB("i32.sub", 0x6B, WasmI32, WasmI32, WasmI32),
    I32_MUL("i32.mul", 0x6C, WasmI32, WasmI32, WasmI32),
    I32_DIV_S("i32.div_s", 0x6D, WasmI32, WasmI32, WasmI32),
    I32_DIV_U("i32.div_u", 0x6E, WasmI32, WasmI32, WasmI32),
    I32_REM_S("i32.rem_s", 0x6F, WasmI32, WasmI32, WasmI32),
    I32_REM_U("i32.rem_u", 0x70, WasmI32, WasmI32, WasmI32),
    I32_AND("i32.and", 0x71, WasmI32, WasmI32, WasmI32),
    I32_OR("i32.or", 0x72, WasmI32, WasmI32, WasmI32),
    I32_XOR("i32.xor", 0x73, WasmI32, WasmI32, WasmI32),
    I32_SHL("i32.shl", 0x74, WasmI32, WasmI32, WasmI32),
    I32_SHR_S("i32.shr_s", 0x75, WasmI32, WasmI32, WasmI32),
    I32_SHR_U("i32.shr_u", 0x76, WasmI32, WasmI32, WasmI32),
    I32_ROTL("i32.rotl", 0x77, WasmI32, WasmI32, WasmI32),
    I32_ROTR("i32.rotr", 0x78, WasmI32, WasmI32, WasmI32),
    I64_ADD("i64.add", 0x7C, WasmI64, WasmI64, WasmI64),
    I64_SUB("i64.sub", 0x7D, WasmI64, WasmI64, WasmI64),
    I64_MUL("i64.mul", 0x7E, WasmI64, WasmI64, WasmI64),
    I64_DIV_S("i64.div_s", 0x7F, WasmI64, WasmI64, WasmI64),
    I64_DIV_U("i64.div_u", 0x80, WasmI64, WasmI64, WasmI64),
    I64_REM_S("i64.rem_s", 0x81, WasmI64, WasmI64, WasmI64),
    I64_REM_U("i64.rem_u", 0x82, WasmI64, WasmI64, WasmI64),
    I64_AND("i64.and", 0x83, WasmI64, WasmI64, WasmI64),
    I64_OR("i64.or", 0x84, WasmI64, WasmI64, WasmI64),
    I64_XOR("i64.xor", 0x85, WasmI64, WasmI64, WasmI64),
    I64_SHL("i64.shl", 0x86, WasmI64, WasmI64, WasmI64),
    I64_SHR_S("i64.shr_s", 0x87, WasmI64, WasmI64, WasmI64),
    I64_SHR_U("i64.shr_u", 0x88, WasmI64, WasmI64, WasmI64),
    I64_ROTL("i64.rotl", 0x89, WasmI64, WasmI64, WasmI64),
    I64_ROTR("i64.rotr", 0x8A, WasmI64, WasmI64, WasmI64),
    F32_ADD("f32.add", 0x92, WasmF32, WasmF32, WasmF32),
    F32_SUB("f32.sub", 0x93, WasmF32, WasmF32, WasmF32),
    F32_MUL("f32.mul", 0x94, WasmF32, WasmF32, WasmF32),
    F32_DIV("f32.div", 0x95, WasmF32, WasmF32, WasmF32),
    F32_MIN("f32.min", 0x96, WasmF32, WasmF32, WasmF32),
    F32_MAX("f32.max", 0x97, WasmF32, WasmF32, WasmF32),
    F32_COPYSIGN("f32.copysign", 0x98, WasmF32, WasmF32, WasmF32),
    F64_ADD("f64.add", 0xA0, WasmF64, WasmF64, WasmF64),
    F64_SUB("f64.sub", 0xA1, WasmF64, WasmF64, WasmF64),
    F64_MUL("f64.mul", 0xA2, WasmF64, WasmF64, WasmF64),
    F64_DIV("f64.div", 0xA3, WasmF64, WasmF64, WasmF64),
    F64_MIN("f64.min", 0xA4, WasmF64, WasmF64, WasmF64),
    F64_MAX("f64.max", 0xA5, WasmF64, WasmF64, WasmF64),
    F64_COPYSIGN("f64.copysign", 0xA6, WasmF64, WasmF64, WasmF64);

    override val operandsCount = 2
    override fun getOperandType(index: Int): WasmValueType = when (index) {
        0 -> lhsType
        1 -> rhsType
        else -> error("Binary operator operand index $index is out of bounds: 0..1")
    }
}

enum class WasmConstantOp(
    override val mnemonic: String,
    override val opcode: Int,
    override val type: WasmValueType
) : WasmTypedOp {
    I32_CONST("i32.const", 0x41, WasmI32),
    I64_CONST("i64.const", 0x42, WasmI64),
    F32_CONST("f32.const", 0x43, WasmF32),
    F64_CONST("f64.const", 0x44, WasmF64);

    override val operandsCount = 0
    override fun getOperandType(index: Int): WasmValueType =
        error("Constant operations have no operands")
}

enum class WasmLoadOp(
    override val mnemonic: String,
    override val opcode: Int,
    override val type: WasmValueType
) : WasmTypedOp {
    I32_LOAD("i32.load", 0x28, WasmI32),
    I64_LOAD("i64.load", 0x29, WasmI64),
    F32_LOAD("f32.load", 0x2A, WasmF32),
    F64_LOAD("f64.load", 0x2B, WasmF64),
    I32_LOAD8_S("i32.load8_s", 0x2C, WasmI32),
    I32_LOAD8_U("i32.load8_u", 0x2D, WasmI32),
    I32_LOAD16_S("i32.load16_s", 0x2E, WasmI32),
    I32_LOAD16_U("i32.load16_u", 0x2F, WasmI32),
    I64_LOAD8_S("i64.load8_s", 0x30, WasmI64),
    I64_LOAD8_U("i64.load8_u", 0x31, WasmI64),
    I64_LOAD16_S("i64.load16_s", 0x32, WasmI64),
    I64_LOAD16_U("i64.load16_u", 0x33, WasmI64),
    I64_LOAD32_S("i64.load32_s", 0x34, WasmI64),
    I64_LOAD32_U("i64.load32_u", 0x35, WasmI64);

    override val operandsCount = 1
    override fun getOperandType(index: Int): WasmValueType {
        require(index == 0)
        return WasmI32  // Wasm32 pointer type
    }
}

@Suppress("unused")
enum class WasmStoreOp(
    override val mnemonic: String,
    override val opcode: Int,
    val storedValueType: WasmValueType
) : WasmTypedOp {
    I32_STORE("i32.store", 0x36, WasmI32),
    I64_STORE("i64.store", 0x37, WasmI64),
    F32_STORE("f32.store", 0x38, WasmF32),
    F64_STORE("f64.store", 0x39, WasmF64),
    I32_STORE8("i32.store8", 0x3A, WasmI32),
    I32_STORE16("i32.store16", 0x3B, WasmI32),
    I64_STORE8("i64.store8", 0x3C, WasmI64),
    I64_STORE16("i64.store16", 0x3D, WasmI64),
    I64_STORE32("i64.store32", 0x3E, WasmI64);

    override val type: WasmValueType? = null

    override val operandsCount = 2
    override fun getOperandType(index: Int): WasmValueType = when (index) {
        0 -> WasmI32  // Wasm32 pointer type
        1 -> storedValueType
        else -> error("Store operand index $index is out of bounds: 0..1")
    }
}

enum class WasmControlOp(
    override val mnemonic: String,
    override val opcode: Int
) : WasmOp {
    UNREACHABLE("unreachable", 0x00),
    NOP("nop", 0x01),

    BLOCK("block", 0x02),
    LOOP("loop", 0x03),

    IF("if", 0x04),
    ELSE("else", 0x05),
    END("end", 0x0B),

    BR("br", 0x0C),
    BR_IF("br_if", 0x0D),
    BR_TABLE("br_table", 0x0E),
    RETURN("return", 0x0F),
    CALL("call", 0x10),
    CALL_INDIRECT("call_indirect", 0x11);
}

enum class WasmParametricOp(
    override val mnemonic: String,
    override val opcode: Int
) : WasmOp {
    DROP("drop", 0x1A),
    SELECT("select", 0x1B);
}

enum class WasmVariableOp(
    override val mnemonic: String,
    override val opcode: Int
) : WasmOp {
    LOCAL_GET("local.get", 0x20),
    LOCAL_SET("local.set", 0x21),
    LOCAL_TEE("local.tee", 0x22),
    GLOBAL_GET("global.get", 0x23),
    GLOBAL_SET("global.set", 0x24);
}

enum class WasmStructOp(
    override val mnemonic: String,
    override val opcode: Int
) : WasmOp {
    STRUCT_NEW("struct.new", 0xFB_00),
    STRUCT_GET("struct.get", 0xFB_03),
    STRUCT_SET("struct.set", 0xFB_06),
    STRUCT_NARROW("struct.narrow", 0xFB_41);
}

enum class WasmRefOp(
    override val mnemonic: String,
    override val opcode: Int,
    val operandTypes: List<WasmValueType>,
    override val type: WasmValueType?
) : WasmTypedOp {
    REF_NULL("ref.null", 0xD0, emptyList(), WasmNullRefType),
    REF_IS_NULL("ref.is_null", 0xD1, listOf(WasmAnyRef), WasmI1),
    REF_EQ("ref.eq", 0xF0, listOf(WasmAnyRef, WasmAnyRef), WasmI1);

    override val operandsCount: Int = operandTypes.size
    override fun getOperandType(index: Int): WasmValueType = operandTypes[index]
}