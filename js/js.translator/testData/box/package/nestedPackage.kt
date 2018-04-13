// EXPECTED_REACHABLE_NODES: 1108
package foo.bar

inline fun <T> run(block: () -> T) = block()

fun box(): String {
    return run {
        "OK"
    }
}