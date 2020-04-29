interface A {
    fun x()
}

class B(val c: Int) : A {
    override fun x() { print("b") }
}

class C(val d: Int) : A {
    override fun x() { print("c") }
}

<warning descr="SSR">class D(b: B) : A by b</warning>

class E(b: C) : A by b

class F(val b: B) : A {
    override fun x() { print("f") }
}

interface G {
    fun x()
}

class H(val c: Int) : G {
    override fun x() { print("h") }
}

class I(b: H) : G by b