// EXPECTED_REACHABLE_NODES: 498
package foo

var global: String = ""

fun bar(s: String): Int {
    global += s
    return 1
}
fun testWhen() {
    global = ""
    when(arrayOf(bar("A"),2,3)) {
        arrayOf(1) -> println("1")
        arrayOf(2) -> println("2")
        else  -> println("else")
    }
    assertEquals("A", global)

}

fun testIntrinsic() {
    global = ""
    val x = arrayOf(bar("A")) == try { arrayOf(bar("B")) } finally {}
    assertEquals("AB", global)
}

fun testElvis() {
    global = ""
    var x = arrayOf(bar("A")) ?: 10
    assertEquals("A", global)
}

fun box(): String {
    testWhen()
    testIntrinsic()
    testElvis()

    return "OK"
}
