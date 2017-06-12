// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {
    var sum = 0
    var sumInner = 0
    var sumOuter = 0

    val range = 0..10
    val skipInner = 5
    val skipOuter = 8


    block@ for (i in range) {
        sum += i

        if (i == skipOuter) break@block

        js("""
            block: {
                if (i === skipInner) break block;

                sumInner += i
            }
        """)

        sumOuter += i
    }

    assertEquals(sum - skipOuter, sumOuter, "sumOuter")
    assertEquals(sum - skipOuter - skipInner, sumInner, "sumInner")

    return "OK"
}