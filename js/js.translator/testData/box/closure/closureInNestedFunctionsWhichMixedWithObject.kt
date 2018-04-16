// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1115
package foo

fun box(): String {
    val t = myRun {
        object {
            fun boo(param: String): String {
                return myRun { param }
            }
        }
    }

    return t.boo("OK")
}
