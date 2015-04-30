// KT-3998 Infix call doesn't work for function literals and another classes which implements invoke convention  (in JS backend)
package foo

class A(val s: String) {
    fun A.invoke(a: A) = "${this.s} ${this@A.s} ${a.s}"
}

fun test1(): String {
    val a = A("a")
    val b = A("b")
    val c = A("c")
    val f = a.b(c) // works
    val s = a b c  //compiler crashes
    return "$f | $s"
}

fun test2(): String {
    val a: (@extension Function2<*, *, *>).(@extension Function2<*, *, *>)->String = { "a" }
    val b: (@extension Function2<*, *, *>).(@extension Function2<*, *, *>)->String = {
        val aa = this as @extension Function2<Any?, Any?, Any?>;
        val cc = it as @extension Function2<Any?, Any?, Any?>;
        "${null.aa(null)} b ${null.cc(null)}"
    }
    val c: (@extension Function2<*, *, *>).(@extension Function2<*, *, *>)->String = { "c" }

    val f = a.b(c) // works
    val s = a b c  //compiler crashes
    return "$f | $s"
}

fun box(): String {
    assertEquals("a b c | a b c", test1())
    assertEquals("a b c | a b c", test2())

    return "OK"
}
