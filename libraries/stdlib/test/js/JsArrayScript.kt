package jstest

fun testSize(): Int {
    val a1 = array<String>()
    val a2 = array("foo")
    val a3 = array("foo", "bar")

    return a1.size() + a2.size() + a3.size()
}

fun testToListToString(): String {
    val a1 = array<String>()
    val a2 = array("foo")
    val a3 = array("foo", "bar")

    return a1.toList().toString() + "-" + a2.toList().toString() + "-" + a3.toList().toString()
}
