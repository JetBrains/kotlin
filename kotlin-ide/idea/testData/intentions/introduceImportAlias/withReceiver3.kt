// WITH_RUNTIME
package my.sample

fun test() {
    0.let { true }.let<caret> { println(it) }
}