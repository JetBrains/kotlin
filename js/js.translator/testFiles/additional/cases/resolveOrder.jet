fun box() : String {
    if (!B().test()) return "fail 1";
    if (!D().test()) return "fail 2"
    if (!F().test()) return "fail 3"
    if (!L().test()) return "fail 4"
    if (!H().test()) return "fail 5"
    if (!N().test()) return "fail 6"
    return "OK"
}

class A {
    fun foo() = 1
}

class B {
    fun foo() = 2

    fun A.bar() = foo()

    fun test() = A().bar() == 1
}


class C {
    fun D.foo() = 2
}

class D {
    fun C.foo() = 1

    fun C.bar() = foo()

    fun test() = C().bar() == 1
}

class E
fun E.foo() = 2

class F {
    fun foo() = 1

    fun E.bar() = foo()

    fun test() = E().bar() == 1
}

class G
fun G.foo() = 2

class H {
    fun G.foo() = 1

    fun G.bar() = foo()

    fun test() = G().bar() == 1
}

class K
class L {
    fun K.bar() = foo()

    fun test() = K().bar() == 1
}
fun K.foo() = 1
fun L.foo() = 2

class M
class N {
    fun foo() = 1
    fun M.foo() = 2

    fun M.bar() = foo()

    fun test() = M().bar() == 1
}
