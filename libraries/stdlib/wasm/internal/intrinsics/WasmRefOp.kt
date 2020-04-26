
@file:ExcludedFromCodegen
@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "unused")

package kotlin.wasm.internal

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@ExcludedFromCodegen
internal annotation class WasmRefOp(val name: String) {
    companion object {
    const val REF_NULL = "REF_NULL"
    const val REF_IS_NULL = "REF_IS_NULL"
    const val REF_EQ = "REF_EQ"
    }
}

@WasmRefOp(WasmRefOp.REF_NULL)
external fun wasm_ref_null(): Nothing?

@WasmRefOp(WasmRefOp.REF_IS_NULL)
external fun wasm_ref_is_null(a: Any?): Boolean

@WasmRefOp(WasmRefOp.REF_EQ)
external fun wasm_ref_eq(a: Any?, b: Any?): Boolean
