interface A {
    fun x(): String
}

class B(val c: Int) : A {
    override fun x(): String { return "$c" }
}

class C(val d: Int) : A {
    override fun x(): String { return "$d" }
}

<warning descr="SSR">class D(b: B) : A by b</warning>

class E(b: C) : A by b

class F(val b: B) : A {
    override fun x(): String { return "$b" }
}

interface G {
    fun x(): String
}

class H(val c: Int) : G {
    override fun x(): String { return "$c" }
}

class I(b: H) : G by b