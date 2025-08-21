// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// LANGUAGE: +ContextParameters
class Scope {
    fun foo() = js("return 'OK';")
}

context(scope: Scope)
fun test() = scope.foo()

fun box(): String {
    with(Scope()) {
        return test()
    }
}