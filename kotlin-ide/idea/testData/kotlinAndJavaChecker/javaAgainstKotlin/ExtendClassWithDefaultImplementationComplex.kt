package test

interface A {
    fun a() = Unit
}

open class AI : A

interface B : A {
    fun b() = Unit
}

open class BI: B

interface C: B {
    fun c() = Unit
}

interface D {
    fun d() = Unit
}

interface S {
    fun a() = Unit
    fun b() = Unit
}