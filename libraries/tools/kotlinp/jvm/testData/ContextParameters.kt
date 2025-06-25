// LANGUAGE: +ContextParameters

interface A
interface B

class C {
    context(x: A, y: B, _: Any) fun f() {}
}

context(x: A, _: Any) fun g() {}
context(y: B, _: Any) val h: Int get() = 42
