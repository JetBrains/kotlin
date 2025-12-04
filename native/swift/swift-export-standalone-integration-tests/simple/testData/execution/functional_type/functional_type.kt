// KIND: STANDALONE
// MODULE: FunctionalType
// FILE: functional_type_simple_consume.kt

private lateinit var block: ()->Unit
fun call_consumed_simple_block() = block()
fun foo_consume_simple(block_input: ()->Unit): Unit {
    block = block_input
}

// FILE: functional_type_simple_produce.kt

var foo_produce_simple_counter: Int = 0
fun foo_produce_simple(): ()->Unit = {
    foo_produce_simple_counter += 1
}

var foo_produce_simple_int_counter: Int = 0
fun foo_produce_simple_int(): ()->Int = {
    foo_produce_simple_int_counter += 1
    foo_produce_simple_int_counter
}

// FILE: functional_type_typealias.kt

typealias callback = () -> Unit
private var callback_property: callback = {}
fun save_typealiased_callback(input: callback) {
    callback_property = input
}
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

// FILE: functional_type_produce_optional_id.kt

var last_seen_bar_by_produceOptionalId: Bar? = null
fun produceOptionalId(): (Bar?)->Bar? = {
    last_seen_bar_by_produceOptionalId = it
    if (it != null) Bar(it.i + 1) else null
}

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

// FILE: functional_type_produce_collection.kt

var last_seen_bar_by_produceListId: List<Int>? = null
fun produceListId(): (List<Int>)->List<Int> = {
    last_seen_bar_by_produceListId = it
    it.asReversed()
}

// MODULE: receivers(data)
// EXPORT_TO_SWIFT
// FILE: functional_type_with_receiver.kt

fun fooReceiverInt(i: Int.()->Unit): Unit = with(5) { i() }

fun fooReceiverString(i: String?.()->Unit): Unit = with("hello") { i() }

fun fooReceiverBar(i: Bar.()->Unit): Unit = with(Bar(5)) { i() }

fun fooReceiverList(i: List<Int>.()->Unit): Unit = with(listOf(1, 2, 3)) { i() }

// FILE: functional_type_produce_withStringReceiver.kt

var last_seen_bar_by_produceWithStringReceiver: String? = null
fun produceWithStringReceiver(): String.()->String = {
    last_seen_bar_by_produceWithStringReceiver = this
    "$this$this"
}

// MODULE: data
// EXPORT_TO_SWIFT
// FILE: data.kt

class Bar(var i: Int)
