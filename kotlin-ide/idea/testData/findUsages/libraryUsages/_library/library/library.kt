package library;

open class A(n: Int) {
    constructor(): this(1)

    open class T(n: Int) {
        constructor(): this(1)

        fun bar(b: Int): Int = b
    }

    fun foo(a: Int): Int = a

    companion object {

    }
}

class B: A {
    constructor(n: Int): super(n)

    class U: A.T {
        constructor(n: Int): super(n)
    }
}

class C(): A(1) {
    class V(): A.T(1)
}

class BB: A {
    constructor(): super()

    class UU: A.T {
        constructor(): super()
    }
}

class CC(): A() {
    class VV(): A.T()
}

fun foo() {

}

object O {

}

fun test() {
    foo()
    val f = ::foo

    val o = O

    val a = A(1)
    val aa = A()

    a.foo(2)
    val ff = A::foo

    val t = A.T(1)
    val tt = A.T()
    t.bar(2)
    val fff = A.T::bar

    class LocalClass(val localClassProperty: Int) {
    }

    LocalClass(localClassProperty = 1).localClassProperty
}

enum class E {
    ONE,
    TWO
}