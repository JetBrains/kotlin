package test

annotation class A0
annotation class A1(val s: String)
annotation class A2(val s: String, val i: Int)

annotation class Foo(val value: String = "foo")
annotation class Bar(val foo: Foo = Foo("foo in bar"))

fun funWithDefaultParams(val s1: String, val s2: String = "s2", val i1: Int, val i2: Int = -1) {}

@A0
@A1("A1 on C")
@A2("A2 on C", 1)
@Foo
@Bar
class C {
    @A0
    @A1("A1 on C.f")
    @A2("A2 on C.f", 2)
    @Foo("foo on C.f")
    @Bar(Foo())
    fun f() {}

    @A0
    @A1("A1 on C.v")
    @A2("A2 on C.v", 3)
    @Foo("foo on C.v")
    @Bar(Foo("foo in Bar on C.v"))
    val v = Double.NaN
}

fun test() {
    val a0 = A0()
    val a1 = A1("A1 in test")
    val a2 = A2("A2 in test", 4)
    val foo1 = Foo()
    val foo2 = Foo("foo in test")
    val bar1 = Bar()
    val bar2 = Bar(Foo())
    val bar3 = Bar(Foo("foo in Bar in test"))

    // to compare IrAnnotation calls with default args to simple function calls with default args:
    funWithDefaultParams(s1 = "s1", s2 = "s2", i1 = "i1", i2 = "i2")
    funWithDefaultParams(s1 = "s1", i1 = "i1")
}
