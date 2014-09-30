package foo

fun box(): String {
    fun simple(s: String? = null): String {
        if (s != null) return s

        return run {
            simple("OK")
        }
    }

    if (simple("OK") != "OK") return "failed on simple recursion"

    val ok = "OK"
    fun withClosure(s: String? = null): String {
        if (s != null) return s

        return ok + run {
            withClosure(ok)
        }
    }

    if (withClosure() != ok + ok) return "failed when closure something"

    return "OK"
}
