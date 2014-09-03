package foo

var global: String = ""

fun bar(s: String): Int {
    global += s
    return 1
}
fun testWhen() {
    global = ""
    when(array(bar("A"),2,3)) {
        array(1) -> println("1")
        array(2) -> println("2")
        else  -> println("else")
    }
    assertEquals("A", global)

}

fun testIntrinsic() {
    global = ""
    val x = array(bar("A")) == try { array(bar("B")) } finally {}
    assertEquals("AB", global)
}

fun testElvis() {
    global = ""
    var x = array(bar("A")) ?: 10
    assertEquals("A", global)
}

fun box(): String {
    testWhen()
    testIntrinsic()
    testElvis()

    return "OK"
}
