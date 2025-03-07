// KIND: STANDALONE
// MODULE: FunctionalType
// FILE: functional_type_simple_produce.kt

private var i: Int = 0

fun read(): Int = i

private var firstCall = true
fun produceClosureIncrementingI(): () -> Unit = if (firstCall) {
    firstCall = false
    { i += 1 }
} else {
    {}
}

// FILE: functional_type_simple_consume.kt

private lateinit var block: ()->Unit
fun call_consumed_simple_block() = block()
fun foo_consume_simple(block_input: ()->Unit): Unit {
    block = block_input
}

// FILE: functional_type_var.kt

var closure_property: () -> Unit = {}
fun call_saved_closure() = closure_property()

// FILE: functional_type_typealias.kt

typealias callback = () -> Unit
var callback_property: callback = {}
fun call_saved_callback() = callback_property()

// MODULE: ref_types(data)
// EXPORT_TO_SWIFT
// FILE: ref_types.kt

private lateinit var ref_block: (Bar) -> Bar

fun saveRefBlock(block: (Bar) -> Bar) {
    ref_block = block
}
fun callRefBlock(with: Bar): Bar = ref_block(with)

// MODULE: optional_types(data)
// EXPORT_TO_SWIFT
// FILE: optional_types.kt

private lateinit var optional_ref_block: (Bar?) -> Bar?

fun saveOptRefBlock(block: (Bar?) -> Bar?) {
    optional_ref_block = block
}
fun callOptRefBlock(with: Bar?): Bar? = optional_ref_block(with)

private lateinit var optional_prim_block: (Int?) -> Int?

fun saveOptPrimBlock(block: (Int?) -> Int?) {
    optional_prim_block = block
}
fun callOptPrimBlock(with: Int?): Int? = optional_prim_block(with)

// MODULE: primitive_types
// EXPORT_TO_SWIFT
// FILE: primitive_types.kt

private lateinit var prim_block: (Byte) -> Byte

fun savePrimBlock(block: (Byte) -> Byte) {
    prim_block = block
}
fun callPrimBlock(with: Byte): Byte = prim_block(with)

// MODULE: collection_types
// EXPORT_TO_SWIFT
// FILE: collection_types.kt

private lateinit var list_block: (List<Int>) -> List<Int>

fun saveListBlock(block: (List<Int>) -> List<Int>) {
    list_block = block
}
fun callListBlock(with: List<Int>): List<Int> = list_block(with)

// MODULE: data
// EXPORT_TO_SWIFT
// FILE: data.kt

class Bar(var i: Int)
