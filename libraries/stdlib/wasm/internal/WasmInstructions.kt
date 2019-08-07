/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:ExcludedFromCodegen
@file:Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "unused")

package kotlin.wasm.internal

@WasmInstruction(WasmInstruction.UNREACHABLE)
external fun wasm_unreachable(): Nothing

@WasmInstruction(WasmInstruction.I32_EQZ)
external fun wasm_i32_eqz(a: Int): Int

@WasmInstruction(WasmInstruction.I32_EQ)
external fun wasm_i32_eq(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_NE)
external fun wasm_i32_ne(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_LT_S)
external fun wasm_i32_lt_s(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_LT_U)
external fun wasm_i32_lt_u(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_GT_S)
external fun wasm_i32_gt_s(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_GT_U)
external fun wasm_i32_gt_u(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_LE_S)
external fun wasm_i32_le_s(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_LE_U)
external fun wasm_i32_le_u(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_GE_S)
external fun wasm_i32_ge_s(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_GE_U)
external fun wasm_i32_ge_u(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I64_EQZ)
external fun wasm_i64_eqz(a: Long): Int

@WasmInstruction(WasmInstruction.I64_EQ)
external fun wasm_i64_eq(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.I64_NE)
external fun wasm_i64_ne(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.I64_LT_S)
external fun wasm_i64_lt_s(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.I64_LT_U)
external fun wasm_i64_lt_u(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.I64_GT_S)
external fun wasm_i64_gt_s(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.I64_GT_U)
external fun wasm_i64_gt_u(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.I64_LE_S)
external fun wasm_i64_le_s(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.I64_LE_U)
external fun wasm_i64_le_u(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.I64_GE_S)
external fun wasm_i64_ge_s(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.I64_GE_U)
external fun wasm_i64_ge_u(a: Long, b: Long): Int

@WasmInstruction(WasmInstruction.F32_EQ)
external fun wasm_f32_eq(a: Float, b: Float): Int

@WasmInstruction(WasmInstruction.F32_NE)
external fun wasm_f32_ne(a: Float, b: Float): Int

@WasmInstruction(WasmInstruction.F32_LT)
external fun wasm_f32_lt(a: Float, b: Float): Int

@WasmInstruction(WasmInstruction.F32_GT)
external fun wasm_f32_gt(a: Float, b: Float): Int

@WasmInstruction(WasmInstruction.F32_LE)
external fun wasm_f32_le(a: Float, b: Float): Int

@WasmInstruction(WasmInstruction.F32_GE)
external fun wasm_f32_ge(a: Float, b: Float): Int

@WasmInstruction(WasmInstruction.F64_EQ)
external fun wasm_f64_eq(a: Double, b: Double): Int

@WasmInstruction(WasmInstruction.F64_NE)
external fun wasm_f64_ne(a: Double, b: Double): Int

@WasmInstruction(WasmInstruction.F64_LT)
external fun wasm_f64_lt(a: Double, b: Double): Int

@WasmInstruction(WasmInstruction.F64_GT)
external fun wasm_f64_gt(a: Double, b: Double): Int

@WasmInstruction(WasmInstruction.F64_LE)
external fun wasm_f64_le(a: Double, b: Double): Int

@WasmInstruction(WasmInstruction.F64_GE)
external fun wasm_f64_ge(a: Double, b: Double): Int

@WasmInstruction(WasmInstruction.I32_CLZ)
external fun wasm_i32_clz(a: Int): Int

@WasmInstruction(WasmInstruction.I32_CTZ)
external fun wasm_i32_ctz(a: Int): Int

@WasmInstruction(WasmInstruction.I32_POPCNT)
external fun wasm_i32_popcnt(a: Int): Int

@WasmInstruction(WasmInstruction.I32_ADD)
external fun wasm_i32_add(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_SUB)
external fun wasm_i32_sub(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_MUL)
external fun wasm_i32_mul(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_DIV_S)
external fun wasm_i32_div_s(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_DIV_U)
external fun wasm_i32_div_u(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_REM_S)
external fun wasm_i32_rem_s(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_REM_U)
external fun wasm_i32_rem_u(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_AND)
external fun wasm_i32_and(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_OR)
external fun wasm_i32_or(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_XOR)
external fun wasm_i32_xor(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_SHL)
external fun wasm_i32_shl(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_SHR_S)
external fun wasm_i32_shr_s(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_SHR_U)
external fun wasm_i32_shr_u(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_ROTL)
external fun wasm_i32_rotl(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I32_ROTR)
external fun wasm_i32_rotr(a: Int, b: Int): Int

@WasmInstruction(WasmInstruction.I64_CLZ)
external fun wasm_i64_clz(a: Long): Long

@WasmInstruction(WasmInstruction.I64_CTZ)
external fun wasm_i64_ctz(a: Long): Long

@WasmInstruction(WasmInstruction.I64_POPCNT)
external fun wasm_i64_popcnt(a: Long): Long

@WasmInstruction(WasmInstruction.I64_ADD)
external fun wasm_i64_add(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_SUB)
external fun wasm_i64_sub(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_MUL)
external fun wasm_i64_mul(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_DIV_S)
external fun wasm_i64_div_s(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_DIV_U)
external fun wasm_i64_div_u(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_REM_S)
external fun wasm_i64_rem_s(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_REM_U)
external fun wasm_i64_rem_u(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_AND)
external fun wasm_i64_and(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_OR)
external fun wasm_i64_or(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_XOR)
external fun wasm_i64_xor(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_SHL)
external fun wasm_i64_shl(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_SHR_S)
external fun wasm_i64_shr_s(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_SHR_U)
external fun wasm_i64_shr_u(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_ROTL)
external fun wasm_i64_rotl(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.I64_ROTR)
external fun wasm_i64_rotr(a: Long, b: Long): Long

@WasmInstruction(WasmInstruction.F32_ABS)
external fun wasm_f32_abs(a: Float): Float

@WasmInstruction(WasmInstruction.F32_NEG)
external fun wasm_f32_neg(a: Float): Float

@WasmInstruction(WasmInstruction.F32_CEIL)
external fun wasm_f32_ceil(a: Float): Float

@WasmInstruction(WasmInstruction.F32_FLOOR)
external fun wasm_f32_floor(a: Float): Float

@WasmInstruction(WasmInstruction.F32_TRUNC)
external fun wasm_f32_trunc(a: Float): Float

@WasmInstruction(WasmInstruction.F32_NEAREST)
external fun wasm_f32_nearest(a: Float): Float

@WasmInstruction(WasmInstruction.F32_SQRT)
external fun wasm_f32_sqrt(a: Float): Float

@WasmInstruction(WasmInstruction.F32_ADD)
external fun wasm_f32_add(a: Float, b: Float): Float

@WasmInstruction(WasmInstruction.F32_SUB)
external fun wasm_f32_sub(a: Float, b: Float): Float

@WasmInstruction(WasmInstruction.F32_MUL)
external fun wasm_f32_mul(a: Float, b: Float): Float

@WasmInstruction(WasmInstruction.F32_DIV)
external fun wasm_f32_div(a: Float, b: Float): Float

@WasmInstruction(WasmInstruction.F32_FMIN)
external fun wasm_f32_fmin(a: Float, b: Float): Float

@WasmInstruction(WasmInstruction.F32_FMAX)
external fun wasm_f32_fmax(a: Float, b: Float): Float

@WasmInstruction(WasmInstruction.F32_COPYSIGN)
external fun wasm_f32_copysign(a: Float, b: Float): Float

@WasmInstruction(WasmInstruction.F64_ABS)
external fun wasm_f64_abs(a: Double): Double

@WasmInstruction(WasmInstruction.F64_NEG)
external fun wasm_f64_neg(a: Double): Double

@WasmInstruction(WasmInstruction.F64_CEIL)
external fun wasm_f64_ceil(a: Double): Double

@WasmInstruction(WasmInstruction.F64_FLOOR)
external fun wasm_f64_floor(a: Double): Double

@WasmInstruction(WasmInstruction.F64_TRUNC)
external fun wasm_f64_trunc(a: Double): Double

@WasmInstruction(WasmInstruction.F64_NEAREST)
external fun wasm_f64_nearest(a: Double): Double

@WasmInstruction(WasmInstruction.F64_SQRT)
external fun wasm_f64_sqrt(a: Double): Double

@WasmInstruction(WasmInstruction.F64_ADD)
external fun wasm_f64_add(a: Double, b: Double): Double

@WasmInstruction(WasmInstruction.F64_SUB)
external fun wasm_f64_sub(a: Double, b: Double): Double

@WasmInstruction(WasmInstruction.F64_MUL)
external fun wasm_f64_mul(a: Double, b: Double): Double

@WasmInstruction(WasmInstruction.F64_DIV)
external fun wasm_f64_div(a: Double, b: Double): Double

@WasmInstruction(WasmInstruction.F64_FMIN)
external fun wasm_f64_fmin(a: Double, b: Double): Double

@WasmInstruction(WasmInstruction.F64_FMAX)
external fun wasm_f64_fmax(a: Double, b: Double): Double

@WasmInstruction(WasmInstruction.F64_COPYSIGN)
external fun wasm_f64_copysign(a: Double, b: Double): Double

@WasmInstruction(WasmInstruction.I32_WRAP_I64)
external fun wasm_i32_wrap_i64(a: Long): Int

@WasmInstruction(WasmInstruction.I32_TRUNC_F32_S)
external fun wasm_i32_trunc_f32_s(a: Float): Int

@WasmInstruction(WasmInstruction.I32_TRUNC_F32_U)
external fun wasm_i32_trunc_f32_u(a: Float): Int

@WasmInstruction(WasmInstruction.I32_TRUNC_F64_S)
external fun wasm_i32_trunc_f64_s(a: Double): Int

@WasmInstruction(WasmInstruction.I32_TRUNC_F64_U)
external fun wasm_i32_trunc_f64_u(a: Double): Int

@WasmInstruction(WasmInstruction.I64_EXTEND_I32_S)
external fun wasm_i64_extend_i32_s(a: Int): Long

@WasmInstruction(WasmInstruction.I64_EXTEND_I32_U)
external fun wasm_i64_extend_i32_u(a: Int): Long

@WasmInstruction(WasmInstruction.I64_TRUNC_F32_S)
external fun wasm_i64_trunc_f32_s(a: Float): Long

@WasmInstruction(WasmInstruction.I64_TRUNC_F32_U)
external fun wasm_i64_trunc_f32_u(a: Float): Long

@WasmInstruction(WasmInstruction.I64_TRUNC_F64_S)
external fun wasm_i64_trunc_f64_s(a: Double): Long

@WasmInstruction(WasmInstruction.I64_TRUNC_F64_U)
external fun wasm_i64_trunc_f64_u(a: Double): Long

@WasmInstruction(WasmInstruction.F32_CONVERT_I32_S)
external fun wasm_f32_convert_i32_s(a: Int): Float

@WasmInstruction(WasmInstruction.F32_CONVERT_I32_U)
external fun wasm_f32_convert_i32_u(a: Int): Float

@WasmInstruction(WasmInstruction.F32_CONVERT_I64_S)
external fun wasm_f32_convert_i64_s(a: Long): Float

@WasmInstruction(WasmInstruction.F32_CONVERT_I64_U)
external fun wasm_f32_convert_i64_u(a: Long): Float

@WasmInstruction(WasmInstruction.F32_DEMOTE_F64)
external fun wasm_f32_demote_f64(a: Double): Float

@WasmInstruction(WasmInstruction.F64_CONVERT_I32_S)
external fun wasm_f64_convert_i32_s(a: Int): Double

@WasmInstruction(WasmInstruction.F64_CONVERT_I32_U)
external fun wasm_f64_convert_i32_u(a: Int): Double

@WasmInstruction(WasmInstruction.F64_CONVERT_I64_S)
external fun wasm_f64_convert_i64_s(a: Long): Double

@WasmInstruction(WasmInstruction.F64_CONVERT_I64_U)
external fun wasm_f64_convert_i64_u(a: Long): Double

@WasmInstruction(WasmInstruction.F64_PROMOTE_F32)
external fun wasm_f64_promote_f32(a: Float): Double

@WasmInstruction(WasmInstruction.I32_REINTERPRET_F32)
external fun wasm_i32_reinterpret_f32(a: Float): Int

@WasmInstruction(WasmInstruction.I64_REINTERPRET_F64)
external fun wasm_i64_reinterpret_f64(a: Double): Long

@WasmInstruction(WasmInstruction.F32_REINTERPRET_I32)
external fun wasm_f32_reinterpret_i32(a: Int): Float

@WasmInstruction(WasmInstruction.F32_CONST_NAN)
external fun wasm_f32_const_nan(): Float

@WasmInstruction(WasmInstruction.F32_CONST_PLUS_INF)
external fun wasm_f32_const_plus_inf(): Float

@WasmInstruction(WasmInstruction.F32_CONST_MINUS_INF)
external fun wasm_f32_const_minus_inf(): Float

@WasmInstruction(WasmInstruction.F64_CONST_NAN)
external fun wasm_f64_const_nan(): Double

@WasmInstruction(WasmInstruction.F64_CONST_PLUS_INF)
external fun wasm_f64_const_plus_inf(): Float

@WasmInstruction(WasmInstruction.F64_CONST_MINUS_INF)
external fun wasm_f64_const_minus_inf(): Float

