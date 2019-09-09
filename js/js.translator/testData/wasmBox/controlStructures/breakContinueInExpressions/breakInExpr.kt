fun test(str: String): String {
    var s = ""
    var i = 0
    while (i < 1) {
        s += if (i<2) str else break
        i++
    }
    return s
}

fun box(): String = test("OK")