// WITH_RUNTIME
package my.sample

val Any.foo: Any get() = this
val Any.bar: Any get() = this

fun test() {
    1.foo.foo.bar<caret>.foo.bar.toString()
}