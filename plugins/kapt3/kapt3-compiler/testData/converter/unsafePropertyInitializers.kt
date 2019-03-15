object Foo {
    const val aString: String = "foo"
    const val aInt: Int = 3

    val bString: String = "bar"
    val bInt: Int = 5

    var cString: String = "baz"
    var cInt: Int = 7

    val d = Boo.z
    val e = Boo.z.length
    val f = 5 + 3
    val g = "a" + "b"
    val h = -4
    val i = Int.MAX_VALUE
    val j = "$e$g"
    val k = g + j
}

object Boo {
    val z = foo()
    fun foo() = "abc"
}

class Zoo(val p: Int = 100)