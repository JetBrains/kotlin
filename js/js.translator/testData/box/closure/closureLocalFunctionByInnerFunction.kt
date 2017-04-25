// EXPECTED_REACHABLE_NODES: 493
package foo

fun box(): String {
    fun simple(s: String? = null): String {
        if (s != null) return s

        return myRun {
            simple("OK")
        }
    }

    if (simple("OK") != "OK") return "failed on simple recursion"

    val ok = "OK"
    fun withClosure(s: String? = null): String {
        if (s != null) return s

        return ok + myRun {
            withClosure(ok)
        }
    }

    if (withClosure() != ok + ok) return "failed when closure something"

    return "OK"
}
