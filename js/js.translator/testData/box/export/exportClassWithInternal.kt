// FILE: lib.kt
@JsExport
class Foo(internal val constructorParameter: String)

// FILE: main.kt
fun box():String {
    val foo = Foo("foo")
    if (foo.constructorParameter != "foo") return "fail"

    return "OK"
}