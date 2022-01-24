// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1523

//@library
//@JsName("imulEmulated")
//internal fun imul(Int, Int): Int

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun imul32(a: Int, b: Int): Int = imul(a, b)

fun imul64(a: Int, b: Int): Int = (a.toLong() * b.toLong()).toInt()

fun generateSeries(data: List<Int>): List<Pair<Int, Int>> {
    val result = mutableListOf<Pair<Int, Int>>()
    for (i in data.indices) {
        for (j in i..data.lastIndex) {
            result += Pair(data[i], data[j])
        }
    }
    return result
}

fun test(series: List<Pair<Int, Int>>): String? {
    for ((a, b) in series) {
        (testSingle(a, b) ?: testSingle(a, -b) ?: testSingle(-a, b) ?: testSingle(-a, -b))?.let { return it }
    }
    return null
}

fun testSingle(a: Int, b: Int): String? {
    var actual = imul32(a, b)
    var expected = imul64(a, b)
    return if (actual != expected) "$a * $b = $expected, got $actual" else null
}

fun box(): String {
    val error = test(generateSeries(listOf(3, 0x1234, 0xFFFF, 0xABCD, 0xABCDEF, 0x43211234, 0x7FFFFFFC, 0x7FFFFFFF)))
    if (error != null) return "fail: error"
    return "OK"
}