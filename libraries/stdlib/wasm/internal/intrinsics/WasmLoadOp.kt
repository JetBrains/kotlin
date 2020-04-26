
@file:ExcludedFromCodegen
@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "unused")

package kotlin.wasm.internal

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@ExcludedFromCodegen
internal annotation class WasmLoadOp(val name: String) {
    companion object {
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
    }
}

@WasmLoadOp(WasmLoadOp.I32_LOAD)
external fun wasm_i32_load(a: Int): Int

@WasmLoadOp(WasmLoadOp.I64_LOAD)
external fun wasm_i64_load(a: Int): Long

@WasmLoadOp(WasmLoadOp.F32_LOAD)
external fun wasm_f32_load(a: Int): Float

@WasmLoadOp(WasmLoadOp.F64_LOAD)
external fun wasm_f64_load(a: Int): Double

@WasmLoadOp(WasmLoadOp.I32_LOAD8_S)
external fun wasm_i32_load8_s(a: Int): Int

@WasmLoadOp(WasmLoadOp.I32_LOAD8_U)
external fun wasm_i32_load8_u(a: Int): Int

@WasmLoadOp(WasmLoadOp.I32_LOAD16_S)
external fun wasm_i32_load16_s(a: Int): Int

@WasmLoadOp(WasmLoadOp.I32_LOAD16_U)
external fun wasm_i32_load16_u(a: Int): Int

@WasmLoadOp(WasmLoadOp.I64_LOAD8_S)
external fun wasm_i64_load8_s(a: Int): Long

@WasmLoadOp(WasmLoadOp.I64_LOAD8_U)
external fun wasm_i64_load8_u(a: Int): Long

@WasmLoadOp(WasmLoadOp.I64_LOAD16_S)
external fun wasm_i64_load16_s(a: Int): Long

@WasmLoadOp(WasmLoadOp.I64_LOAD16_U)
external fun wasm_i64_load16_u(a: Int): Long

@WasmLoadOp(WasmLoadOp.I64_LOAD32_S)
external fun wasm_i64_load32_s(a: Int): Long

@WasmLoadOp(WasmLoadOp.I64_LOAD32_U)
external fun wasm_i64_load32_u(a: Int): Long
