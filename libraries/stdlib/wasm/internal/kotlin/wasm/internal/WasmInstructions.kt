/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:ExcludedFromCodegen
@file:Suppress("unused", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "INLINE_CLASS_IN_EXTERNAL_DECLARATION")


package kotlin.wasm.internal

@WasmOp(WasmOp.UNREACHABLE)
internal fun wasm_unreachable(): Nothing =
    implementedAsIntrinsic

internal fun wasm_float_nan(): Float =
    implementedAsIntrinsic

internal fun wasm_double_nan(): Double =
    implementedAsIntrinsic

internal fun <From, To> wasm_ref_cast(a: From): To =
    implementedAsIntrinsic

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