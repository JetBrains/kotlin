// EXPECTED_REACHABLE_NODES: 487
// http://youtrack.jetbrains.com/issue/KT-5594
// JS: compiler crashes

package foo

fun bar(f: () -> Unit) {
}

fun test() {
    bar {
        // val actionId: Any = 1
        val item: Any? = 1
        if (item != null) {
            // In original version, as I remember, `when` was an important to reproduce, but now it is not.
            // when(actionId){
            //     1 -> { 1 }
            //     "2" -> { "2"}
            //      else -> {}
            // }
        }
    }
    bar {
        val actionId: Any = 1
        val item: Any? = 1
        if (item != null) {
            when (actionId) {
                1 -> {
                    1
                }
                "2" -> {
                    "2"
                }
                else -> {
                }
            }
        }
    }

}

fun box(): String {
    return "OK"
}