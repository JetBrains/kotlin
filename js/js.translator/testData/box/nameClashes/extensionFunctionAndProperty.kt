// EXPECTED_REACHABLE_NODES: 896
package foo

public fun <T> List<T>.some(): T = this[0]
public fun String.some(): Char = this[0]
public val <T> List<T>.some: T get() = this[1]
public val String.some: Char get() = this[1]

fun box(): String {

    val data = listOf("foo", "bar")

    assertEquals("foo", data.some())
    assertEquals("bar", data.some)
    assertEquals('f', "foo".some())
    assertEquals('a', "bar".some)

    return "OK"
}