/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:ExcludedFromCodegen
@file:Suppress(
    "INLINE_CLASS_IN_EXTERNAL_DECLARATION",
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "unused"
)

package kotlin.wasm.internal

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmOp(val name: String) {
    companion object {
        const val I32_EQZ = "I32_EQZ"
        const val I64_EQZ = "I64_EQZ"
        const val I32_CLZ = "I32_CLZ"
        const val I32_CTZ = "I32_CTZ"
        const val I32_POPCNT = "I32_POPCNT"
        const val I64_CLZ = "I64_CLZ"
        const val I64_CTZ = "I64_CTZ"
        const val I64_POPCNT = "I64_POPCNT"
        const val F32_ABS = "F32_ABS"
        const val F32_NEG = "F32_NEG"
        const val F32_CEIL = "F32_CEIL"
        const val F32_FLOOR = "F32_FLOOR"
        const val F32_TRUNC = "F32_TRUNC"
        const val F32_NEAREST = "F32_NEAREST"
        const val F32_SQRT = "F32_SQRT"
        const val F64_ABS = "F64_ABS"
        const val F64_NEG = "F64_NEG"
        const val F64_CEIL = "F64_CEIL"
        const val F64_FLOOR = "F64_FLOOR"
        const val F64_TRUNC = "F64_TRUNC"
        const val F64_NEAREST = "F64_NEAREST"
        const val F64_SQRT = "F64_SQRT"
        const val I32_WRAP_I64 = "I32_WRAP_I64"
        const val I32_TRUNC_F32_S = "I32_TRUNC_F32_S"
        const val I32_TRUNC_F32_U = "I32_TRUNC_F32_U"
        const val I32_TRUNC_F64_S = "I32_TRUNC_F64_S"
        const val I32_TRUNC_F64_U = "I32_TRUNC_F64_U"
        const val I64_EXTEND_I32_S = "I64_EXTEND_I32_S"
        const val I64_EXTEND_I32_U = "I64_EXTEND_I32_U"
        const val I64_TRUNC_F32_S = "I64_TRUNC_F32_S"
        const val I64_TRUNC_F32_U = "I64_TRUNC_F32_U"
        const val I64_TRUNC_F64_S = "I64_TRUNC_F64_S"
        const val I64_TRUNC_F64_U = "I64_TRUNC_F64_U"
        const val F32_CONVERT_I32_S = "F32_CONVERT_I32_S"
        const val F32_CONVERT_I32_U = "F32_CONVERT_I32_U"
        const val F32_CONVERT_I64_S = "F32_CONVERT_I64_S"
        const val F32_CONVERT_I64_U = "F32_CONVERT_I64_U"
        const val F32_DEMOTE_F64 = "F32_DEMOTE_F64"
        const val F64_CONVERT_I32_S = "F64_CONVERT_I32_S"
        const val F64_CONVERT_I32_U = "F64_CONVERT_I32_U"
        const val F64_CONVERT_I64_S = "F64_CONVERT_I64_S"
        const val F64_CONVERT_I64_U = "F64_CONVERT_I64_U"
        const val F64_PROMOTE_F32 = "F64_PROMOTE_F32"
        const val I32_REINTERPRET_F32 = "I32_REINTERPRET_F32"
        const val I64_REINTERPRET_F64 = "I64_REINTERPRET_F64"
        const val F32_REINTERPRET_I32 = "F32_REINTERPRET_I32"
        const val F64_REINTERPRET_I64 = "F64_REINTERPRET_I64"
        const val I32_EXTEND8_S = "I32_EXTEND8_S"
        const val I32_EXTEND16_S = "I32_EXTEND16_S"
        const val I64_EXTEND8_S = "I64_EXTEND8_S"
        const val I64_EXTEND16_S = "I64_EXTEND16_S"
        const val I64_EXTEND32_S = "I64_EXTEND32_S"
        const val I32_TRUNC_SAT_F32_S = "I32_TRUNC_SAT_F32_S"
        const val I32_TRUNC_SAT_F32_U = "I32_TRUNC_SAT_F32_U"
        const val I32_TRUNC_SAT_F64_S = "I32_TRUNC_SAT_F64_S"
        const val I32_TRUNC_SAT_F64_U = "I32_TRUNC_SAT_F64_U"
        const val I64_TRUNC_SAT_F32_S = "I64_TRUNC_SAT_F32_S"
        const val I64_TRUNC_SAT_F32_U = "I64_TRUNC_SAT_F32_U"
        const val I64_TRUNC_SAT_F64_S = "I64_TRUNC_SAT_F64_S"
        const val I64_TRUNC_SAT_F64_U = "I64_TRUNC_SAT_F64_U"
        const val I32_EQ = "I32_EQ"
        const val I32_NE = "I32_NE"
        const val I32_LT_S = "I32_LT_S"
        const val I32_LT_U = "I32_LT_U"
        const val I32_GT_S = "I32_GT_S"
        const val I32_GT_U = "I32_GT_U"
        const val I32_LE_S = "I32_LE_S"
        const val I32_LE_U = "I32_LE_U"
        const val I32_GE_S = "I32_GE_S"
        const val I32_GE_U = "I32_GE_U"
        const val I64_EQ = "I64_EQ"
        const val I64_NE = "I64_NE"
        const val I64_LT_S = "I64_LT_S"
        const val I64_LT_U = "I64_LT_U"
        const val I64_GT_S = "I64_GT_S"
        const val I64_GT_U = "I64_GT_U"
        const val I64_LE_S = "I64_LE_S"
        const val I64_LE_U = "I64_LE_U"
        const val I64_GE_S = "I64_GE_S"
        const val I64_GE_U = "I64_GE_U"
        const val F32_EQ = "F32_EQ"
        const val F32_NE = "F32_NE"
        const val F32_LT = "F32_LT"
        const val F32_GT = "F32_GT"
        const val F32_LE = "F32_LE"
        const val F32_GE = "F32_GE"
        const val F64_EQ = "F64_EQ"
        const val F64_NE = "F64_NE"
        const val F64_LT = "F64_LT"
        const val F64_GT = "F64_GT"
        const val F64_LE = "F64_LE"
        const val F64_GE = "F64_GE"
        const val I32_ADD = "I32_ADD"
        const val I32_SUB = "I32_SUB"
        const val I32_MUL = "I32_MUL"
        const val I32_DIV_S = "I32_DIV_S"
        const val I32_DIV_U = "I32_DIV_U"
        const val I32_REM_S = "I32_REM_S"
        const val I32_REM_U = "I32_REM_U"
        const val I32_AND = "I32_AND"
        const val I32_OR = "I32_OR"
        const val I32_XOR = "I32_XOR"
        const val I32_SHL = "I32_SHL"
        const val I32_SHR_S = "I32_SHR_S"
        const val I32_SHR_U = "I32_SHR_U"
        const val I32_ROTL = "I32_ROTL"
        const val I32_ROTR = "I32_ROTR"
        const val I64_ADD = "I64_ADD"
        const val I64_SUB = "I64_SUB"
        const val I64_MUL = "I64_MUL"
        const val I64_DIV_S = "I64_DIV_S"
        const val I64_DIV_U = "I64_DIV_U"
        const val I64_REM_S = "I64_REM_S"
        const val I64_REM_U = "I64_REM_U"
        const val I64_AND = "I64_AND"
        const val I64_OR = "I64_OR"
        const val I64_XOR = "I64_XOR"
        const val I64_SHL = "I64_SHL"
        const val I64_SHR_S = "I64_SHR_S"
        const val I64_SHR_U = "I64_SHR_U"
        const val I64_ROTL = "I64_ROTL"
        const val I64_ROTR = "I64_ROTR"
        const val F32_ADD = "F32_ADD"
        const val F32_SUB = "F32_SUB"
        const val F32_MUL = "F32_MUL"
        const val F32_DIV = "F32_DIV"
        const val F32_MIN = "F32_MIN"
        const val F32_MAX = "F32_MAX"
        const val F32_COPYSIGN = "F32_COPYSIGN"
        const val F64_ADD = "F64_ADD"
        const val F64_SUB = "F64_SUB"
        const val F64_MUL = "F64_MUL"
        const val F64_DIV = "F64_DIV"
        const val F64_MIN = "F64_MIN"
        const val F64_MAX = "F64_MAX"
        const val F64_COPYSIGN = "F64_COPYSIGN"
        const val I32_CONST = "I32_CONST"
        const val I64_CONST = "I64_CONST"
        const val F32_CONST = "F32_CONST"
        const val F64_CONST = "F64_CONST"
        const val I32_LOAD = "I32_LOAD"
        const val I64_LOAD = "I64_LOAD"
        const val F32_LOAD = "F32_LOAD"
        const val F64_LOAD = "F64_LOAD"
        const val I32_LOAD8_S = "I32_LOAD8_S"
        const val I32_LOAD8_U = "I32_LOAD8_U"
        const val I32_LOAD16_S = "I32_LOAD16_S"
        const val I32_LOAD16_U = "I32_LOAD16_U"
        const val I64_LOAD8_S = "I64_LOAD8_S"
        const val I64_LOAD8_U = "I64_LOAD8_U"
        const val I64_LOAD16_S = "I64_LOAD16_S"
        const val I64_LOAD16_U = "I64_LOAD16_U"
        const val I64_LOAD32_S = "I64_LOAD32_S"
        const val I64_LOAD32_U = "I64_LOAD32_U"
        const val I32_STORE = "I32_STORE"
        const val I64_STORE = "I64_STORE"
        const val F32_STORE = "F32_STORE"
        const val F64_STORE = "F64_STORE"
        const val I32_STORE8 = "I32_STORE8"
        const val I32_STORE16 = "I32_STORE16"
        const val I64_STORE8 = "I64_STORE8"
        const val I64_STORE16 = "I64_STORE16"
        const val I64_STORE32 = "I64_STORE32"
        const val MEMORY_SIZE = "MEMORY_SIZE"
        const val MEMORY_GROW = "MEMORY_GROW"
        const val MEMORY_INIT = "MEMORY_INIT"
        const val DATA_DROP = "DATA_DROP"
        const val MEMORY_COPY = "MEMORY_COPY"
        const val MEMORY_FILL = "MEMORY_FILL"
        const val TABLE_GET = "TABLE_GET"
        const val TABLE_SET = "TABLE_SET"
        const val TABLE_GROW = "TABLE_GROW"
        const val TABLE_SIZE = "TABLE_SIZE"
        const val TABLE_FILL = "TABLE_FILL"
        const val TABLE_INIT = "TABLE_INIT"
        const val ELEM_DROP = "ELEM_DROP"
        const val TABLE_COPY = "TABLE_COPY"
        const val UNREACHABLE = "UNREACHABLE"
        const val NOP = "NOP"
        const val BLOCK = "BLOCK"
        const val LOOP = "LOOP"
        const val IF = "IF"
        const val ELSE = "ELSE"
        const val END = "END"
        const val BR = "BR"
        const val BR_IF = "BR_IF"
        const val BR_TABLE = "BR_TABLE"
        const val RETURN = "RETURN"
        const val CALL = "CALL"
        const val CALL_INDIRECT = "CALL_INDIRECT"
        const val DROP = "DROP"
        const val SELECT = "SELECT"
        const val SELECT_TYPED = "SELECT_TYPED"
        const val LOCAL_GET = "LOCAL_GET"
        const val LOCAL_SET = "LOCAL_SET"
        const val LOCAL_TEE = "LOCAL_TEE"
        const val GLOBAL_GET = "GLOBAL_GET"
        const val GLOBAL_SET = "GLOBAL_SET"
        const val REF_NULL = "REF_NULL"
        const val REF_IS_NULL = "REF_IS_NULL"
        const val REF_EQ = "REF_EQ"
        const val REF_FUNC = "REF_FUNC"
        const val STRUCT_NEW_WITH_RTT = "STRUCT_NEW_WITH_RTT"
        const val STRUCT_GET = "STRUCT_GET"
        const val STRUCT_SET = "STRUCT_SET"
        const val REF_CAST = "REF_CAST"
        const val RTT_CANON = "RTT_CANON"
        const val RTT_SUB = "RTT_SUB"
    }
}

@WasmOp(WasmOp.I32_EQ)
public external fun wasm_i32_eq(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I32_NE)
public external fun wasm_i32_ne(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I32_LT_S)
public external fun wasm_i32_lt_s(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I32_LT_U)
public external fun wasm_i32_lt_u(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I32_GT_S)
public external fun wasm_i32_gt_s(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I32_GT_U)
public external fun wasm_i32_gt_u(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I32_LE_S)
public external fun wasm_i32_le_s(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I32_LE_U)
public external fun wasm_i32_le_u(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I32_GE_S)
public external fun wasm_i32_ge_s(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I32_GE_U)
public external fun wasm_i32_ge_u(a: Int, b: Int): Boolean

@WasmOp(WasmOp.I64_EQ)
public external fun wasm_i64_eq(a: Long, b: Long): Boolean

@WasmOp(WasmOp.I64_NE)
public external fun wasm_i64_ne(a: Long, b: Long): Boolean

@WasmOp(WasmOp.I64_LT_S)
public external fun wasm_i64_lt_s(a: Long, b: Long): Boolean

@WasmOp(WasmOp.I64_LT_U)
public external fun wasm_i64_lt_u(a: Long, b: Long): Boolean

@WasmOp(WasmOp.I64_GT_S)
public external fun wasm_i64_gt_s(a: Long, b: Long): Boolean

@WasmOp(WasmOp.I64_GT_U)
public external fun wasm_i64_gt_u(a: Long, b: Long): Boolean

@WasmOp(WasmOp.I64_LE_S)
public external fun wasm_i64_le_s(a: Long, b: Long): Boolean

@WasmOp(WasmOp.I64_LE_U)
public external fun wasm_i64_le_u(a: Long, b: Long): Boolean

@WasmOp(WasmOp.I64_GE_S)
public external fun wasm_i64_ge_s(a: Long, b: Long): Boolean

@WasmOp(WasmOp.I64_GE_U)
public external fun wasm_i64_ge_u(a: Long, b: Long): Boolean

@WasmOp(WasmOp.F32_EQ)
public external fun wasm_f32_eq(a: Float, b: Float): Boolean

@WasmOp(WasmOp.F32_NE)
public external fun wasm_f32_ne(a: Float, b: Float): Boolean

@WasmOp(WasmOp.F32_LT)
public external fun wasm_f32_lt(a: Float, b: Float): Boolean

@WasmOp(WasmOp.F32_GT)
public external fun wasm_f32_gt(a: Float, b: Float): Boolean

@WasmOp(WasmOp.F32_LE)
public external fun wasm_f32_le(a: Float, b: Float): Boolean

@WasmOp(WasmOp.F32_GE)
public external fun wasm_f32_ge(a: Float, b: Float): Boolean

@WasmOp(WasmOp.F64_EQ)
public external fun wasm_f64_eq(a: Double, b: Double): Boolean

@WasmOp(WasmOp.F64_NE)
public external fun wasm_f64_ne(a: Double, b: Double): Boolean

@WasmOp(WasmOp.F64_LT)
public external fun wasm_f64_lt(a: Double, b: Double): Boolean

@WasmOp(WasmOp.F64_GT)
public external fun wasm_f64_gt(a: Double, b: Double): Boolean

@WasmOp(WasmOp.F64_LE)
public external fun wasm_f64_le(a: Double, b: Double): Boolean

@WasmOp(WasmOp.F64_GE)
public external fun wasm_f64_ge(a: Double, b: Double): Boolean

@WasmOp(WasmOp.I32_ADD)
public external fun wasm_i32_add(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_SUB)
public external fun wasm_i32_sub(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_MUL)
public external fun wasm_i32_mul(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_DIV_S)
public external fun wasm_i32_div_s(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_DIV_U)
public external fun wasm_i32_div_u(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_REM_S)
public external fun wasm_i32_rem_s(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_REM_U)
public external fun wasm_i32_rem_u(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_AND)
public external fun wasm_i32_and(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_OR)
public external fun wasm_i32_or(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_XOR)
public external fun wasm_i32_xor(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_SHL)
public external fun wasm_i32_shl(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_SHR_S)
public external fun wasm_i32_shr_s(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_SHR_U)
public external fun wasm_i32_shr_u(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_ROTL)
public external fun wasm_i32_rotl(a: Int, b: Int): Int

@WasmOp(WasmOp.I32_ROTR)
public external fun wasm_i32_rotr(a: Int, b: Int): Int

@WasmOp(WasmOp.I64_ADD)
public external fun wasm_i64_add(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_SUB)
public external fun wasm_i64_sub(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_MUL)
public external fun wasm_i64_mul(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_DIV_S)
public external fun wasm_i64_div_s(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_DIV_U)
public external fun wasm_i64_div_u(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_REM_S)
public external fun wasm_i64_rem_s(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_REM_U)
public external fun wasm_i64_rem_u(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_AND)
public external fun wasm_i64_and(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_OR)
public external fun wasm_i64_or(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_XOR)
public external fun wasm_i64_xor(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_SHL)
public external fun wasm_i64_shl(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_SHR_S)
public external fun wasm_i64_shr_s(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_SHR_U)
public external fun wasm_i64_shr_u(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_ROTL)
public external fun wasm_i64_rotl(a: Long, b: Long): Long

@WasmOp(WasmOp.I64_ROTR)
public external fun wasm_i64_rotr(a: Long, b: Long): Long

@WasmOp(WasmOp.F32_ADD)
public external fun wasm_f32_add(a: Float, b: Float): Float

@WasmOp(WasmOp.F32_SUB)
public external fun wasm_f32_sub(a: Float, b: Float): Float

@WasmOp(WasmOp.F32_MUL)
public external fun wasm_f32_mul(a: Float, b: Float): Float

@WasmOp(WasmOp.F32_DIV)
public external fun wasm_f32_div(a: Float, b: Float): Float

@WasmOp(WasmOp.F32_MIN)
public external fun wasm_f32_min(a: Float, b: Float): Float

@WasmOp(WasmOp.F32_MAX)
public external fun wasm_f32_max(a: Float, b: Float): Float

@WasmOp(WasmOp.F32_COPYSIGN)
public external fun wasm_f32_copysign(a: Float, b: Float): Float

@WasmOp(WasmOp.F64_ADD)
public external fun wasm_f64_add(a: Double, b: Double): Double

@WasmOp(WasmOp.F64_SUB)
public external fun wasm_f64_sub(a: Double, b: Double): Double

@WasmOp(WasmOp.F64_MUL)
public external fun wasm_f64_mul(a: Double, b: Double): Double

@WasmOp(WasmOp.F64_DIV)
public external fun wasm_f64_div(a: Double, b: Double): Double

@WasmOp(WasmOp.F64_MIN)
public external fun wasm_f64_min(a: Double, b: Double): Double

@WasmOp(WasmOp.F64_MAX)
public external fun wasm_f64_max(a: Double, b: Double): Double


@WasmOp(WasmOp.REF_IS_NULL)
public external fun wasm_ref_is_null(a: Any?): Boolean

@WasmOp(WasmOp.REF_EQ)
public external fun wasm_ref_eq(a: Any?, b: Any?): Boolean


// ---

@WasmOp(WasmOp.F32_NEAREST)
public external fun wasm_f32_nearest(a: Float): Float

@WasmOp(WasmOp.F64_NEAREST)
public external fun wasm_f64_nearest(a: Double): Double

@WasmOp(WasmOp.I32_WRAP_I64)
public external fun wasm_i32_wrap_i64(a: Long): Int

@WasmOp(WasmOp.I64_EXTEND_I32_S)
public external fun wasm_i64_extend_i32_s(a: Int): Long

@WasmOp(WasmOp.F32_CONVERT_I32_S)
public external fun wasm_f32_convert_i32_s(a: Int): Float

@WasmOp(WasmOp.F32_CONVERT_I64_S)
public external fun wasm_f32_convert_i64_s(a: Long): Float

@WasmOp(WasmOp.F32_DEMOTE_F64)
public external fun wasm_f32_demote_f64(a: Double): Float

@WasmOp(WasmOp.F64_CONVERT_I32_S)
public external fun wasm_f64_convert_i32_s(a: Int): Double

@WasmOp(WasmOp.F64_CONVERT_I64_S)
public external fun wasm_f64_convert_i64_s(a: Long): Double

@WasmOp(WasmOp.F64_PROMOTE_F32)
public external fun wasm_f64_promote_f32(a: Float): Double

@WasmOp(WasmOp.I32_REINTERPRET_F32)
public external fun wasm_i32_reinterpret_f32(a: Float): Int

@WasmOp(WasmOp.F32_REINTERPRET_I32)
public external fun wasm_f32_reinterpret_i32(a: Int): Float

@WasmOp(WasmOp.I32_TRUNC_SAT_F32_S)
public external fun wasm_i32_trunc_sat_f32_s(a: Float): Int

@WasmOp(WasmOp.I32_TRUNC_SAT_F64_S)
public external fun wasm_i32_trunc_sat_f64_s(a: Double): Int

@WasmOp(WasmOp.I64_TRUNC_SAT_F32_S)
public external fun wasm_i64_trunc_sat_f32_s(a: Float): Long

@WasmOp(WasmOp.I64_TRUNC_SAT_F64_S)
public external fun wasm_i64_trunc_sat_f64_s(a: Double): Long

@WasmOp(WasmOp.I32_LOAD)
public external fun wasm_i32_load(x: Int): Int