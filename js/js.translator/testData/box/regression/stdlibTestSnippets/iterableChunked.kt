// EXPECTED_REACHABLE_NODES: 1750
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    val size = 2
    val a = Array(size) { "$it" }
    val data = Iterable { a.iterator() }

    val dataChunked = data.chunked(size).single()
    val expectedSingleChunk = data.toList()
    if (expectedSingleChunk != dataChunked)
        return "Fail"

    return "OK"
}