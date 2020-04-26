
@file:ExcludedFromCodegen
@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "unused")

package kotlin.wasm.internal

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@ExcludedFromCodegen
internal annotation class WasmUnaryOp(val name: String) {
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
    const val I32_TRUNC_SAT_F32_S = "I32_TRUNC_SAT_F32_S"
    const val I32_TRUNC_SAT_F32_U = "I32_TRUNC_SAT_F32_U"
    const val I32_TRUNC_SAT_F64_S = "I32_TRUNC_SAT_F64_S"
    const val I32_TRUNC_SAT_F64_U = "I32_TRUNC_SAT_F64_U"
    const val I64_TRUNC_SAT_F32_S = "I64_TRUNC_SAT_F32_S"
    const val I64_TRUNC_SAT_F32_U = "I64_TRUNC_SAT_F32_U"
    const val I64_TRUNC_SAT_F64_S = "I64_TRUNC_SAT_F64_S"
    const val I64_TRUNC_SAT_F64_U = "I64_TRUNC_SAT_F64_U"
    }
}

@WasmUnaryOp(WasmUnaryOp.I32_EQZ)
external fun wasm_i32_eqz(a: Boolean): Boolean

@WasmUnaryOp(WasmUnaryOp.I64_EQZ)
external fun wasm_i64_eqz(a: Int): Long

@WasmUnaryOp(WasmUnaryOp.I32_CLZ)
external fun wasm_i32_clz(a: Int): Int

@WasmUnaryOp(WasmUnaryOp.I32_CTZ)
external fun wasm_i32_ctz(a: Int): Int

@WasmUnaryOp(WasmUnaryOp.I32_POPCNT)
external fun wasm_i32_popcnt(a: Int): Int

@WasmUnaryOp(WasmUnaryOp.I64_CLZ)
external fun wasm_i64_clz(a: Long): Long

@WasmUnaryOp(WasmUnaryOp.I64_CTZ)
external fun wasm_i64_ctz(a: Long): Long

@WasmUnaryOp(WasmUnaryOp.I64_POPCNT)
external fun wasm_i64_popcnt(a: Long): Long

@WasmUnaryOp(WasmUnaryOp.F32_ABS)
external fun wasm_f32_abs(a: Float): Float

@WasmUnaryOp(WasmUnaryOp.F32_NEG)
external fun wasm_f32_neg(a: Float): Float

@WasmUnaryOp(WasmUnaryOp.F32_CEIL)
external fun wasm_f32_ceil(a: Float): Float

@WasmUnaryOp(WasmUnaryOp.F32_FLOOR)
external fun wasm_f32_floor(a: Float): Float

@WasmUnaryOp(WasmUnaryOp.F32_TRUNC)
external fun wasm_f32_trunc(a: Float): Float

@WasmUnaryOp(WasmUnaryOp.F32_NEAREST)
external fun wasm_f32_nearest(a: Float): Float

@WasmUnaryOp(WasmUnaryOp.F32_SQRT)
external fun wasm_f32_sqrt(a: Float): Float

@WasmUnaryOp(WasmUnaryOp.F64_ABS)
external fun wasm_f64_abs(a: Double): Double

@WasmUnaryOp(WasmUnaryOp.F64_NEG)
external fun wasm_f64_neg(a: Double): Double

@WasmUnaryOp(WasmUnaryOp.F64_CEIL)
external fun wasm_f64_ceil(a: Double): Double

@WasmUnaryOp(WasmUnaryOp.F64_FLOOR)
external fun wasm_f64_floor(a: Double): Double

@WasmUnaryOp(WasmUnaryOp.F64_TRUNC)
external fun wasm_f64_trunc(a: Double): Double

@WasmUnaryOp(WasmUnaryOp.F64_NEAREST)
external fun wasm_f64_nearest(a: Double): Double

@WasmUnaryOp(WasmUnaryOp.F64_SQRT)
external fun wasm_f64_sqrt(a: Double): Double

@WasmUnaryOp(WasmUnaryOp.I32_WRAP_I64)
external fun wasm_i32_wrap_i64(a: Long): Int

@WasmUnaryOp(WasmUnaryOp.I32_TRUNC_F32_S)
external fun wasm_i32_trunc_f32_s(a: Float): Int

@WasmUnaryOp(WasmUnaryOp.I32_TRUNC_F32_U)
external fun wasm_i32_trunc_f32_u(a: Float): Int

@WasmUnaryOp(WasmUnaryOp.I32_TRUNC_F64_S)
external fun wasm_i32_trunc_f64_s(a: Double): Int

@WasmUnaryOp(WasmUnaryOp.I32_TRUNC_F64_U)
external fun wasm_i32_trunc_f64_u(a: Double): Int

@WasmUnaryOp(WasmUnaryOp.I64_EXTEND_I32_S)
external fun wasm_i64_extend_i32_s(a: Int): Long

@WasmUnaryOp(WasmUnaryOp.I64_EXTEND_I32_U)
external fun wasm_i64_extend_i32_u(a: Int): Long

@WasmUnaryOp(WasmUnaryOp.I64_TRUNC_F32_S)
external fun wasm_i64_trunc_f32_s(a: Float): Long

@WasmUnaryOp(WasmUnaryOp.I64_TRUNC_F32_U)
external fun wasm_i64_trunc_f32_u(a: Float): Long

@WasmUnaryOp(WasmUnaryOp.I64_TRUNC_F64_S)
external fun wasm_i64_trunc_f64_s(a: Double): Long

@WasmUnaryOp(WasmUnaryOp.I64_TRUNC_F64_U)
external fun wasm_i64_trunc_f64_u(a: Double): Long

@WasmUnaryOp(WasmUnaryOp.F32_CONVERT_I32_S)
external fun wasm_f32_convert_i32_s(a: Int): Float

@WasmUnaryOp(WasmUnaryOp.F32_CONVERT_I32_U)
external fun wasm_f32_convert_i32_u(a: Int): Float

@WasmUnaryOp(WasmUnaryOp.F32_CONVERT_I64_S)
external fun wasm_f32_convert_i64_s(a: Long): Float

@WasmUnaryOp(WasmUnaryOp.F32_CONVERT_I64_U)
external fun wasm_f32_convert_i64_u(a: Long): Float

@WasmUnaryOp(WasmUnaryOp.F32_DEMOTE_F64)
external fun wasm_f32_demote_f64(a: Double): Float

@WasmUnaryOp(WasmUnaryOp.F64_CONVERT_I32_S)
external fun wasm_f64_convert_i32_s(a: Int): Double

@WasmUnaryOp(WasmUnaryOp.F64_CONVERT_I32_U)
external fun wasm_f64_convert_i32_u(a: Int): Double

@WasmUnaryOp(WasmUnaryOp.F64_CONVERT_I64_S)
external fun wasm_f64_convert_i64_s(a: Long): Double

@WasmUnaryOp(WasmUnaryOp.F64_CONVERT_I64_U)
external fun wasm_f64_convert_i64_u(a: Long): Double

@WasmUnaryOp(WasmUnaryOp.F64_PROMOTE_F32)
external fun wasm_f64_promote_f32(a: Float): Double

@WasmUnaryOp(WasmUnaryOp.I32_REINTERPRET_F32)
external fun wasm_i32_reinterpret_f32(a: Float): Int

@WasmUnaryOp(WasmUnaryOp.I64_REINTERPRET_F64)
external fun wasm_i64_reinterpret_f64(a: Double): Long

@WasmUnaryOp(WasmUnaryOp.F32_REINTERPRET_I32)
external fun wasm_f32_reinterpret_i32(a: Int): Float

@WasmUnaryOp(WasmUnaryOp.F64_REINTERPRET_I64)
external fun wasm_f64_reinterpret_i64(a: Long): Double

@WasmUnaryOp(WasmUnaryOp.I32_TRUNC_SAT_F32_S)
external fun wasm_i32_trunc_sat_f32_s(a: Float): Int

@WasmUnaryOp(WasmUnaryOp.I32_TRUNC_SAT_F32_U)
external fun wasm_i32_trunc_sat_f32_u(a: Float): Int

@WasmUnaryOp(WasmUnaryOp.I32_TRUNC_SAT_F64_S)
external fun wasm_i32_trunc_sat_f64_s(a: Double): Int

@WasmUnaryOp(WasmUnaryOp.I32_TRUNC_SAT_F64_U)
external fun wasm_i32_trunc_sat_f64_u(a: Double): Int

@WasmUnaryOp(WasmUnaryOp.I64_TRUNC_SAT_F32_S)
external fun wasm_i64_trunc_sat_f32_s(a: Float): Long

@WasmUnaryOp(WasmUnaryOp.I64_TRUNC_SAT_F32_U)
external fun wasm_i64_trunc_sat_f32_u(a: Float): Long

@WasmUnaryOp(WasmUnaryOp.I64_TRUNC_SAT_F64_S)
external fun wasm_i64_trunc_sat_f64_s(a: Double): Long

@WasmUnaryOp(WasmUnaryOp.I64_TRUNC_SAT_F64_U)
external fun wasm_i64_trunc_sat_f64_u(a: Double): Long
