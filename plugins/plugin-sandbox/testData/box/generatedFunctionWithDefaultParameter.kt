// IGNORE_BACKEND: ANY
// IGNORE_HMPP: ANY
// DUMP_IR
package foo

annotation class MyAnnotation

@MyAnnotation
class Foo

fun box(): String {
    val foo = Foo()
    return foo.idWithDefault()
}
