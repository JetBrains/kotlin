
@file:ExcludedFromCodegen
@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "unused")

package kotlin.wasm.internal

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@ExcludedFromCodegen
internal annotation class WasmStoreOp(val name: String) {
    companion object {
    const val I32_STORE = "I32_STORE"
    const val I64_STORE = "I64_STORE"
    const val F32_STORE = "F32_STORE"
    const val F64_STORE = "F64_STORE"
    const val I32_STORE8 = "I32_STORE8"
    const val I32_STORE16 = "I32_STORE16"
    const val I64_STORE8 = "I64_STORE8"
    const val I64_STORE16 = "I64_STORE16"
    const val I64_STORE32 = "I64_STORE32"
    }
}

@WasmStoreOp(WasmStoreOp.I32_STORE)
external fun wasm_i32_store(a: Int, b: Int): Unit

@WasmStoreOp(WasmStoreOp.I64_STORE)
external fun wasm_i64_store(a: Int, b: Long): Unit

@WasmStoreOp(WasmStoreOp.F32_STORE)
external fun wasm_f32_store(a: Int, b: Float): Unit

@WasmStoreOp(WasmStoreOp.F64_STORE)
external fun wasm_f64_store(a: Int, b: Double): Unit

@WasmStoreOp(WasmStoreOp.I32_STORE8)
external fun wasm_i32_store8(a: Int, b: Int): Unit

@WasmStoreOp(WasmStoreOp.I32_STORE16)
external fun wasm_i32_store16(a: Int, b: Int): Unit

@WasmStoreOp(WasmStoreOp.I64_STORE8)
external fun wasm_i64_store8(a: Int, b: Long): Unit

@WasmStoreOp(WasmStoreOp.I64_STORE16)
external fun wasm_i64_store16(a: Int, b: Long): Unit

@WasmStoreOp(WasmStoreOp.I64_STORE32)
external fun wasm_i64_store32(a: Int, b: Long): Unit
