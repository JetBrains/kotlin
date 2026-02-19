// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// WITH_STDLIB
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: tuples.kt

package foo

@JsExport
class Foo

@JsExport
val pair: Pair<String, Int> = "Test" to 42

@JsExport
val triple: Triple<String, Int, Pair<String, Int>> = Triple("Test", 42, pair)


@JsExport
fun createPair(): Pair<Int, String> =
    42 to "Test"

@JsExport
fun createTriple(): Triple<Foo, Array<Pair<Int, String>>, String> =
    Triple(Foo(), arrayOf(createPair()), "OK")

@JsExport
fun <K, V> acceptPair(somePair: Pair<K, V>): V =
    somePair.second

@JsExport
fun <A, B, C> acceptTriple(someTriple: Triple<A, B, C>): Pair<A, C> =
    someTriple.first to someTriple.third
