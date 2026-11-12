// DUMP_LINE_MAPPINGS

package test

val topLevel: Any field: String = "OK"

class A {
    val a: Any field: String = "OK"

    val list: List<String> field = mutableListOf<String>()

    val delegated: Any by lazy { "OK" }

    companion object {
        val c: CharSequence field: StringBuilder = StringBuilder()
    }
}
