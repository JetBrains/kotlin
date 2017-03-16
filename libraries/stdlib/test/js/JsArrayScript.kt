package test.collections.js

fun testSize(): Int {
    val a1 = arrayOf<String>()
    val a2 = arrayOf("foo")
    val a3 = arrayOf("foo", "bar")

    return a1.size + a2.size + a3.size
}

fun testToListToString(): String {
    val a1 = arrayOf<String>()
    val a2 = arrayOf("foo")
    val a3 = arrayOf("foo", "bar")

    return a1.toList().toString() + "-" + a2.toList().toString() + "-" + a3.toList().toString()
}
