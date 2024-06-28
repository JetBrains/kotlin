// IGNORE_BACKEND: JS_IR_ES6
// ^^^ Muted until the fix of KT-67355

open class A {
    open val foo: String = "O"
}

class B : A() {
    override val foo: String by lazy {
        super.foo + "K"
    }
}

fun box(): String = B().foo
