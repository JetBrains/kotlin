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

// FILE: produce_func_with_ref.kt

fun produce_func_with_ref_id(): (Foo) -> Foo = {
    it.i += 2
    it
}
fun produce_func_with_ref_zip(): (Foo, Foo) -> Bar = { l, r -> Bar(l, r) }

// FILE: consume_func_with_ref.kt

private lateinit var f: Foo

fun save(foo: Foo) { f = foo }
fun read_foo(): Foo { return f }

fun consume_closure_with_ref_id(block: (Foo) -> Foo): Foo {
    return block(f)
}
fun consume_closure_with_ref_zip(block: (Foo) -> Foo): Bar {
    return Bar(f, block(f))
}

lateinit var refId_closure_property: (Foo) -> Foo
fun call_saved_refId_closure(f: Foo) = refId_closure_property(f)

// FILE: data.kt

class Foo(var i: Int)
class Bar(val left: Foo, val right: Foo)
