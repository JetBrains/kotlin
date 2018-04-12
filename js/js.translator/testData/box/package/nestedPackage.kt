// EXPECTED_REACHABLE_NODES: 1108
package foo.bar

inline fun <T> run(block: () -> T) = block()

inline fun id(s: String) = s

fun box(): String {
//    run {
//        return "OK"
//    }

//    run {
//        return id("O") + "K"
//    }

    val a = run {
        "OK"
    }

    return a

}