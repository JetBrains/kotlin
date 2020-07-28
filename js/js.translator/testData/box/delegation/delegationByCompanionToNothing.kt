// EXPECTED_REACHABLE_NODES: 1253
// MODULE: lib
// FILE: lib.kt
interface II {
    companion object : DDD by error("OK")
}

interface DDD {
    fun bar(d: String = error("FAIL4")): String
}

// MODULE: main(lib)
// FILE: main.kt
fun box() : String {
    try {
        return II.bar()
    } catch (e: IllegalStateException) {
        return e.message ?: "FAIL 2"
    }

    return "FAIL"
}