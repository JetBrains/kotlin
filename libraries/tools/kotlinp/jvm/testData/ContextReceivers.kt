// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_BACKEND_K2: JVM_IR

interface A
interface B

context(A) class C {
    context(B) fun f() {}
}

context(A) fun g() {}
context(B) val h: Int get() = 42
