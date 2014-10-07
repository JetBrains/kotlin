package foo

val r = "OK"

fun simple(s: String? = null): String {
    if (s != null) return s

    return run {
        simple("OK")
    }
}

val ok = "OK"
fun withClosure(s: String? = null): String {
    if (s != null) return s

    return ok + run {
        withClosure(ok)
    }
}

fun box(): String {
    if (simple("OK") != "OK") return "failed on simple recursion"

    if (withClosure() != ok + ok) return "failed when closure something"

    return "OK"
}
