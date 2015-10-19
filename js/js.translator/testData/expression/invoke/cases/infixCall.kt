// KT-3998 Infix call doesn't work for function literals and another classes which implements invoke convention  (in JS backend)
package foo

fun test(): String {
    val a: (@Extension Function2<*, *, *>).(@Extension Function2<*, *, *>)->String = { "a" }
    val b: (@Extension Function2<*, *, *>).(@Extension Function2<*, *, *>)->String = {
        val aa = this as @Extension Function2<Any?, Any?, Any?>;
        val cc = it as @Extension Function2<Any?, Any?, Any?>;
        "${null.aa(null)} b ${null.cc(null)}"
    }
    val c: (@Extension Function2<*, *, *>).(@Extension Function2<*, *, *>)->String = { "c" }

    val f = a.b(c) // works
    val s = a b c  //compiler crashes
    return "$f | $s"
}

fun box(): String {
    assertEquals("a b c | a b c", test())

    return "OK"
}
