// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// WITH_STDLIB
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE

// TODO fix statics export in DCE-driven mode
// SKIP_DCE_DRIVEN

// MODULE: JS_TESTS
// FILE: f1.kt

@JsExport
fun provideList(): List<Int> = listOf(1, 2, 3)

@JsExport
fun provideMutableList(): MutableList<Int> = mutableListOf(4, 5, 6)

@JsExport
fun provideSet(): Set<Int> = setOf(1, 2, 3)

@JsExport
fun provideMutableSet(): MutableSet<Int> = mutableSetOf(4, 5, 6)

@JsExport
fun provideMap(): Map<String, Int> = mapOf("a" to 1, "b" to 2, "c" to 3)

@JsExport
fun provideMutableMap(): MutableMap<String, Int> = mutableMapOf("d" to 4, "e" to 5, "f" to 6)

@JsExport
fun consumeList(list: List<Int>) = list.toString() == "[1, 2, 3]"

@JsExport
fun consumeMutableList(list: MutableList<Int>): Boolean {
    list.add(7)
    return list.toString() == "[4, 5, 6, 7]"
}

@JsExport
fun consumeSet(list: Set<Int>) = list.toString() == "[1, 2, 3]"

@JsExport
fun consumeMutableSet(list: MutableSet<Int>): Boolean {
    list.add(7)
    return list.toString() == "[4, 5, 6, 7]"
}

@JsExport
fun consumeMap(map: Map<String, Int>) = map.toString() == "{a=1, b=2, c=3}"

@JsExport
fun consumeMutableMap(map: MutableMap<String, Int>): Boolean {
    map["g"] = 7
    return map.toString() == "{d=4, e=5, f=6, g=7}"
}
