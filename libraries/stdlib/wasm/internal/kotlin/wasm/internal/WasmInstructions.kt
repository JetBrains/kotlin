/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:ExcludedFromCodegen
@file:Suppress("unused", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "INLINE_CLASS_IN_EXTERNAL_DECLARATION", "UNUSED_PARAMETER")


package kotlin.wasm.internal

@WasmOp(WasmOp.UNREACHABLE)
internal fun wasm_unreachable(): Nothing =
    implementedAsIntrinsic

@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
internal fun <reified To> wasm_ref_cast_null(a: Any?): To =
    implementedAsIntrinsic

@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
internal fun <reified To> wasm_ref_test(a: Any?): Boolean =
    implementedAsIntrinsic

@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
@WasmOp(WasmOp.REF_TEST_NULL)
internal fun <reified To> wasm_ref_test_null(a: Any?): Boolean =
    implementedAsIntrinsic

internal fun <T> wasm_array_copy(destination: T, destinationIndex: Int, source: T, sourceIndex: Int, length: Int): Unit =
    implementedAsIntrinsic

internal fun <T> array_new_data0(address: Int, length: Int): T =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_EQ)
internal fun wasm_i32_eq(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_NE)
internal fun wasm_i32_ne(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_LT_S)
internal fun wasm_i32_lt_s(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_LT_U)
internal fun wasm_i32_lt_u(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_GT_S)
internal fun wasm_i32_gt_s(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_GT_U)
internal fun wasm_i32_gt_u(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_LE_S)
internal fun wasm_i32_le_s(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_LE_U)
internal fun wasm_i32_le_u(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_GE_S)
internal fun wasm_i32_ge_s(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_GE_U)
internal fun wasm_i32_ge_u(a: Int, b: Int): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_EQ)
internal fun wasm_i64_eq(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_NE)
internal fun wasm_i64_ne(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_LT_S)
internal fun wasm_i64_lt_s(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_LT_U)
internal fun wasm_i64_lt_u(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_GT_S)
internal fun wasm_i64_gt_s(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_GT_U)
internal fun wasm_i64_gt_u(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_LE_S)
internal fun wasm_i64_le_s(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_LE_U)
internal fun wasm_i64_le_u(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_GE_S)
internal fun wasm_i64_ge_s(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_GE_U)
internal fun wasm_i64_ge_u(a: Long, b: Long): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_EQ)
internal fun wasm_f32_eq(a: Float, b: Float): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_NE)
internal fun wasm_f32_ne(a: Float, b: Float): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_LT)
internal fun wasm_f32_lt(a: Float, b: Float): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_GT)
internal fun wasm_f32_gt(a: Float, b: Float): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_LE)
internal fun wasm_f32_le(a: Float, b: Float): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_GE)
internal fun wasm_f32_ge(a: Float, b: Float): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_EQ)
internal fun wasm_f64_eq(a: Double, b: Double): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_NE)
internal fun wasm_f64_ne(a: Double, b: Double): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_LT)
internal fun wasm_f64_lt(a: Double, b: Double): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_GT)
internal fun wasm_f64_gt(a: Double, b: Double): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_LE)
internal fun wasm_f64_le(a: Double, b: Double): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_GE)
internal fun wasm_f64_ge(a: Double, b: Double): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_ADD)
internal fun wasm_i32_add(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_SUB)
internal fun wasm_i32_sub(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_MUL)
internal fun wasm_i32_mul(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_DIV_S)
internal fun wasm_i32_div_s(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_DIV_U)
internal fun wasm_i32_div_u(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_REM_S)
internal fun wasm_i32_rem_s(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_REM_U)
internal fun wasm_i32_rem_u(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_AND)
internal fun wasm_i32_and(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_OR)
internal fun wasm_i32_or(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_XOR)
internal fun wasm_i32_xor(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_SHL)
internal fun wasm_i32_shl(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_SHR_S)
internal fun wasm_i32_shr_s(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_SHR_U)
internal fun wasm_i32_shr_u(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_ROTL)
internal fun wasm_i32_rotl(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_ROTR)
internal fun wasm_i32_rotr(a: Int, b: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_ADD)
internal fun wasm_i64_add(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_SUB)
internal fun wasm_i64_sub(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_MUL)
internal fun wasm_i64_mul(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_DIV_S)
internal fun wasm_i64_div_s(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_DIV_U)
internal fun wasm_i64_div_u(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_REM_S)
internal fun wasm_i64_rem_s(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_REM_U)
internal fun wasm_i64_rem_u(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_AND)
internal fun wasm_i64_and(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_OR)
internal fun wasm_i64_or(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_XOR)
internal fun wasm_i64_xor(a: Long, b: Long): Long =
    implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_SHL)
internal fun wasm_i64_shl(a: Long, b: Long): Long =
    implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_SHR_S)
internal fun wasm_i64_shr_s(a: Long, b: Long): Long =
    implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_SHR_U)
internal fun wasm_i64_shr_u(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_ROTL)
internal fun wasm_i64_rotl(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_ROTR)
internal fun wasm_i64_rotr(a: Long, b: Long): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_ADD)
internal fun wasm_f32_add(a: Float, b: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_SUB)
internal fun wasm_f32_sub(a: Float, b: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_MUL)
internal fun wasm_f32_mul(a: Float, b: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_DIV)
internal fun wasm_f32_div(a: Float, b: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_MIN)
internal fun wasm_f32_min(a: Float, b: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_MAX)
internal fun wasm_f32_max(a: Float, b: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_COPYSIGN)
internal fun wasm_f32_copysign(a: Float, b: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_ADD)
internal fun wasm_f64_add(a: Double, b: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_SUB)
internal fun wasm_f64_sub(a: Double, b: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_MUL)
internal fun wasm_f64_mul(a: Double, b: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_DIV)
internal fun wasm_f64_div(a: Double, b: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_MIN)
internal fun wasm_f64_min(a: Double, b: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_MAX)
internal fun wasm_f64_max(a: Double, b: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_SQRT)
internal fun wasm_f64_sqrt(a: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_CEIL)
internal fun wasm_f64_ceil(a: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_FLOOR)
internal fun wasm_f64_floor(a: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_TRUNC)
internal fun wasm_f64_truncate(a: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_COPYSIGN)
internal fun wasm_f64_copysign(a: Double, b: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_ABS)
internal fun wasm_f64_abs(a: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_SQRT)
internal fun wasm_f32_sqrt(a: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_CEIL)
internal fun wasm_f32_ceil(a: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_FLOOR)
internal fun wasm_f32_floor(a: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_TRUNC)
internal fun wasm_f32_truncate(a: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_ABS)
internal fun wasm_f32_abs(a: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.REF_IS_NULL)
internal fun wasm_ref_is_null(a: Any?): Boolean =
    implementedAsIntrinsic

@WasmOp(WasmOp.REF_EQ)
internal fun wasm_ref_eq(a: Any?, b: Any?): Boolean =
    implementedAsIntrinsic

// ---

@WasmOp(WasmOp.F32_NEAREST)
internal fun wasm_f32_nearest(a: Float): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_NEAREST)
internal fun wasm_f64_nearest(a: Double): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_WRAP_I64)
internal fun wasm_i32_wrap_i64(a: Long): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_EXTEND_I32_S)
internal fun wasm_i64_extend_i32_s(a: Int): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_CONVERT_I32_S)
internal fun wasm_f32_convert_i32_s(a: Int): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_CONVERT_I64_S)
internal fun wasm_f32_convert_i64_s(a: Long): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_DEMOTE_F64)
internal fun wasm_f32_demote_f64(a: Double): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_CONVERT_I32_S)
internal fun wasm_f64_convert_i32_s(a: Int): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_CONVERT_I64_S)
internal fun wasm_f64_convert_i64_s(a: Long): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_PROMOTE_F32)
internal fun wasm_f64_promote_f32(a: Float): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.F32_REINTERPRET_I32)
internal fun wasm_f32_reinterpret_i32(a: Int): Float =
    implementedAsIntrinsic

@WasmOp(WasmOp.F64_REINTERPRET_I64)
internal fun wasm_f64_reinterpret_i64(a: Long): Double =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_REINTERPRET_F32)
internal fun wasm_i32_reinterpret_f32(a: Float): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_REINTERPRET_F64)
internal fun wasm_i64_reinterpret_f64(a: Double): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_TRUNC_SAT_F32_S)
internal fun wasm_i32_trunc_sat_f32_s(a: Float): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_TRUNC_SAT_F64_S)
internal fun wasm_i32_trunc_sat_f64_s(a: Double): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_TRUNC_SAT_F32_S)
internal fun wasm_i64_trunc_sat_f32_s(a: Float): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_TRUNC_SAT_F64_S)
internal fun wasm_i64_trunc_sat_f64_s(a: Double): Long =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_LOAD)
internal fun wasm_i32_load(x: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_LOAD16_U)
internal fun wasm_i32_load16_u(x: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_STORE)
internal fun wasm_i32_store(addr: Int, i: Int): Unit =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_STORE16)
internal fun wasm_i32_store16(addr: Int, c: Char): Unit =
    implementedAsIntrinsic

@WasmOp(WasmOp.I32_CLZ)
internal fun wasm_i32_clz(a: Int): Int =
    implementedAsIntrinsic

@WasmOp(WasmOp.I64_CLZ)
internal fun wasm_i64_clz(a: Long): Long =
    implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_POPCNT)
internal fun wasm_i64_popcnt(a: Long): Long =
    implementedAsIntrinsic

@PublishedApi
@WasmOp(WasmOp.I64_CTZ)
internal fun wasm_i64_ctz(a: Long): Long =
    implementedAsIntrinsic