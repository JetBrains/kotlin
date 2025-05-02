// LANGUAGE: +ContextParameters

interface A
interface B

class C {
    context(x: A, y: B) fun f() {}
}

context(x: A) fun g() {}
context(y: B) val h: Int get() = 42
