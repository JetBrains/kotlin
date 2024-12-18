// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

fun foo_sus(): suspend ()->Unit = TODO()
fun foo_1(): ()->Unit = TODO()
fun foo_2(): Function0<Unit> = foo_1()

fun foo_consume_simple(block: ()->Unit): Unit = TODO()

var closure_property: () -> Unit = {}
