// EXPECTED_REACHABLE_NODES: 494
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
