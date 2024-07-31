// EXPECTED_REACHABLE_NODES: 1280

inline fun foo() = (object : II {}).ok()

// CHECK_BREAKS_COUNT: function=box count=0
// CHECK_LABELS_COUNT: function=box name=$l$block count=0
fun box() = foo()

interface I {
    fun ok() = "OK"
}

interface II: I