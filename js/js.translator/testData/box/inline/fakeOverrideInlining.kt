// EXPECTED_REACHABLE_NODES: 1280

inline fun foo() = (object : II {}).ok()

fun box() = foo()

interface I {
    fun ok() = "OK"
}

interface II: I