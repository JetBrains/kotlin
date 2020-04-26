
@file:ExcludedFromCodegen
@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "unused")

package kotlin.wasm.internal

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@ExcludedFromCodegen
internal annotation class WasmBinaryOp(val name: String) {
    companion object {
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
    }
}

@WasmBinaryOp(WasmBinaryOp.I32_EQ)
external fun wasm_i32_eq(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_NE)
external fun wasm_i32_ne(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_LT_S)
external fun wasm_i32_lt_s(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_LT_U)
external fun wasm_i32_lt_u(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_GT_S)
external fun wasm_i32_gt_s(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_GT_U)
external fun wasm_i32_gt_u(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_LE_S)
external fun wasm_i32_le_s(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_LE_U)
external fun wasm_i32_le_u(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_GE_S)
external fun wasm_i32_ge_s(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_GE_U)
external fun wasm_i32_ge_u(a: Int, b: Int): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_EQ)
external fun wasm_i64_eq(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_NE)
external fun wasm_i64_ne(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_LT_S)
external fun wasm_i64_lt_s(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_LT_U)
external fun wasm_i64_lt_u(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_GT_S)
external fun wasm_i64_gt_s(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_GT_U)
external fun wasm_i64_gt_u(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_LE_S)
external fun wasm_i64_le_s(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_LE_U)
external fun wasm_i64_le_u(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_GE_S)
external fun wasm_i64_ge_s(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.I64_GE_U)
external fun wasm_i64_ge_u(a: Long, b: Long): Boolean

@WasmBinaryOp(WasmBinaryOp.F32_EQ)
external fun wasm_f32_eq(a: Float, b: Float): Boolean

@WasmBinaryOp(WasmBinaryOp.F32_NE)
external fun wasm_f32_ne(a: Float, b: Float): Boolean

@WasmBinaryOp(WasmBinaryOp.F32_LT)
external fun wasm_f32_lt(a: Float, b: Float): Boolean

@WasmBinaryOp(WasmBinaryOp.F32_GT)
external fun wasm_f32_gt(a: Float, b: Float): Boolean

@WasmBinaryOp(WasmBinaryOp.F32_LE)
external fun wasm_f32_le(a: Float, b: Float): Boolean

@WasmBinaryOp(WasmBinaryOp.F32_GE)
external fun wasm_f32_ge(a: Float, b: Float): Boolean

@WasmBinaryOp(WasmBinaryOp.F64_EQ)
external fun wasm_f64_eq(a: Double, b: Double): Boolean

@WasmBinaryOp(WasmBinaryOp.F64_NE)
external fun wasm_f64_ne(a: Double, b: Double): Boolean

@WasmBinaryOp(WasmBinaryOp.F64_LT)
external fun wasm_f64_lt(a: Double, b: Double): Boolean

@WasmBinaryOp(WasmBinaryOp.F64_GT)
external fun wasm_f64_gt(a: Double, b: Double): Boolean

@WasmBinaryOp(WasmBinaryOp.F64_LE)
external fun wasm_f64_le(a: Double, b: Double): Boolean

@WasmBinaryOp(WasmBinaryOp.F64_GE)
external fun wasm_f64_ge(a: Double, b: Double): Boolean

@WasmBinaryOp(WasmBinaryOp.I32_ADD)
external fun wasm_i32_add(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_SUB)
external fun wasm_i32_sub(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_MUL)
external fun wasm_i32_mul(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_DIV_S)
external fun wasm_i32_div_s(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_DIV_U)
external fun wasm_i32_div_u(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_REM_S)
external fun wasm_i32_rem_s(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_REM_U)
external fun wasm_i32_rem_u(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_AND)
external fun wasm_i32_and(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_OR)
external fun wasm_i32_or(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_XOR)
external fun wasm_i32_xor(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_SHL)
external fun wasm_i32_shl(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_SHR_S)
external fun wasm_i32_shr_s(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_SHR_U)
external fun wasm_i32_shr_u(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_ROTL)
external fun wasm_i32_rotl(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I32_ROTR)
external fun wasm_i32_rotr(a: Int, b: Int): Int

@WasmBinaryOp(WasmBinaryOp.I64_ADD)
external fun wasm_i64_add(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_SUB)
external fun wasm_i64_sub(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_MUL)
external fun wasm_i64_mul(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_DIV_S)
external fun wasm_i64_div_s(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_DIV_U)
external fun wasm_i64_div_u(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_REM_S)
external fun wasm_i64_rem_s(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_REM_U)
external fun wasm_i64_rem_u(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_AND)
external fun wasm_i64_and(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_OR)
external fun wasm_i64_or(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_XOR)
external fun wasm_i64_xor(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_SHL)
external fun wasm_i64_shl(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_SHR_S)
external fun wasm_i64_shr_s(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_SHR_U)
external fun wasm_i64_shr_u(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_ROTL)
external fun wasm_i64_rotl(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.I64_ROTR)
external fun wasm_i64_rotr(a: Long, b: Long): Long

@WasmBinaryOp(WasmBinaryOp.F32_ADD)
external fun wasm_f32_add(a: Float, b: Float): Float

@WasmBinaryOp(WasmBinaryOp.F32_SUB)
external fun wasm_f32_sub(a: Float, b: Float): Float

@WasmBinaryOp(WasmBinaryOp.F32_MUL)
external fun wasm_f32_mul(a: Float, b: Float): Float

@WasmBinaryOp(WasmBinaryOp.F32_DIV)
external fun wasm_f32_div(a: Float, b: Float): Float

@WasmBinaryOp(WasmBinaryOp.F32_MIN)
external fun wasm_f32_min(a: Float, b: Float): Float

@WasmBinaryOp(WasmBinaryOp.F32_MAX)
external fun wasm_f32_max(a: Float, b: Float): Float

@WasmBinaryOp(WasmBinaryOp.F32_COPYSIGN)
external fun wasm_f32_copysign(a: Float, b: Float): Float

@WasmBinaryOp(WasmBinaryOp.F64_ADD)
external fun wasm_f64_add(a: Double, b: Double): Double

@WasmBinaryOp(WasmBinaryOp.F64_SUB)
external fun wasm_f64_sub(a: Double, b: Double): Double

@WasmBinaryOp(WasmBinaryOp.F64_MUL)
external fun wasm_f64_mul(a: Double, b: Double): Double

@WasmBinaryOp(WasmBinaryOp.F64_DIV)
external fun wasm_f64_div(a: Double, b: Double): Double

@WasmBinaryOp(WasmBinaryOp.F64_MIN)
external fun wasm_f64_min(a: Double, b: Double): Double

@WasmBinaryOp(WasmBinaryOp.F64_MAX)
external fun wasm_f64_max(a: Double, b: Double): Double

@WasmBinaryOp(WasmBinaryOp.F64_COPYSIGN)
external fun wasm_f64_copysign(a: Double, b: Double): Double
