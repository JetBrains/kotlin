// EXPECTED_REACHABLE_NODES: 491
package foo

// CHECK_FUNCTIONS_HAVE_SAME_LINES: syntaxTestInline syntaxTest

inline fun syntaxTestInline() {
    var result: Int = -0

    for (i in 1..10)
        result += 1

    for (i in arrayOf(10).indices)
        result += i

    while (result < 30)
        result += 1

    while (true)
        break

    while (false)
        continue

    do
        result += 1
    while (result < 40)

    if (true)
        result += 10

    if (false)
        result += 0
    else
        result += 10

    result += if (true) 10 else 0

    result += if (false) 0 else 10

    when (result) {
        0    -> result += 0
        else -> result += 10
    }

    when (result) {
        result -> result += 10
        else   -> result += 0
    }

    result = result + 10 - 10 * 1 / 1

    try {
        result += 10
    } catch (e: Exception) {
        result += 0
    }

    result++

    --result

    val nullable: String? = null
}

fun syntaxTest() {
    syntaxTestInline()
}

fun box(): String {
    syntaxTest()
    return "OK"
}