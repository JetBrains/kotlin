// KIND: STANDALONE
// MODULE: simple
// EXPORT_TO_SWIFT
// FILE: simple.kt

fun foo_sus(): suspend ()->Unit = TODO()
fun foo_1(): ()->Unit = TODO()
fun foo_2(): Function0<Unit> = foo_1()

fun foo_consume_simple(block: ()->Unit): Unit = TODO()

var closure_property: () -> Unit = {}

// MODULE: ref_types(data)
// EXPORT_TO_SWIFT
// FILE: ref_types.kt

fun produce_block_with_reftype(): Function2<Foo, Bar, Foo> = TODO()

// MODULE: data
// EXPORT_TO_SWIFT
// FILE: data.kt

class Foo
class Bar
