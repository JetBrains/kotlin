// WITH_STDLIB
package foo

fun box(): String {
    val foo: Char? = 'c'
    requireNotNull(foo) { "Fail 1" }
    val bar: Char? = null
    requireNotNull(bar) { return "OK" }
    return "Fail 2"
}
