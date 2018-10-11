// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1286
package foo

class Foo {
    val OK = "OK";
    var result: String = ""
    init {
        fun bar(s: String? = null) {
            if (s != null) {
                result = s
                return
            }

            myRun {
                bar(OK)
            }
        }
        bar();
    }

}

fun box(): String {
    return Foo().result
}
