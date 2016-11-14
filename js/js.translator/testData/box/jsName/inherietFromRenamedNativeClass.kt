package foo

@native
@JsName("A")
open class B(val foo: String)

class C(s: String) : B(s)

fun box(): String {
    return C("OK").foo
}