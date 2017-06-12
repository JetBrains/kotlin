// EXPECTED_REACHABLE_NODES: 917
package foo

object EmptyObject {}

object SomeObject {
    val foo = 1
    var bar = "t"
    fun baz() {}
}

val emptyObjectExpr = object {}

val someObjectExpr = object {
    val foo = 1
    var bar = "t"
    fun baz() {}
}

val o = js("Object")

fun keys(a: Any): List<String> {
    val arr: Array<String> = o.keys(a)
    return arr.toList()
}

fun getOwnPropertyNames(a: Any): List<String> {
    val arr: Array<String> = o.getOwnPropertyNames(a)
    return arr.toList()
}

fun box(): String {
    assertEquals(listOf(), getOwnPropertyNames(EmptyObject))
    assertEquals(listOf(), keys(EmptyObject))

    assertEquals(listOf("foo", "bar"), getOwnPropertyNames(SomeObject))
    assertEquals(listOf("foo", "bar"), keys(SomeObject))

    assertEquals(listOf(), getOwnPropertyNames(emptyObjectExpr))
    assertEquals(listOf(), keys(emptyObjectExpr))

    assertEquals(listOf("foo", "bar"), getOwnPropertyNames(someObjectExpr))
    assertEquals(listOf("foo", "bar"), keys(someObjectExpr))

    return "OK"
}
