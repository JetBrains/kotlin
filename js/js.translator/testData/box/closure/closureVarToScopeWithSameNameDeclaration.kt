// EXPECTED_REACHABLE_NODES: 489
package foo

val f = true

fun box(): String {
    var bar = ""
    var boo = 23

    fun baz() {
        bar += "test "

        if (f) {
            val v1 = 42
            var bar = 12
            bar += v1

            val v2 = 7
            var boo = ""
            boo += v2
        }

        boo += 7
        bar += "text"
    }

    baz()
    if (bar != "test text") return "bar != \"test text\", bar = \"$bar\"";
    if (boo != 30) return "boo != 61, boo = $boo";

    return "OK"
}
