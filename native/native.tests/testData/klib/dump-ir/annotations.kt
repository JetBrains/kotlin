package test

annotation class A0
annotation class A1(val s: String)
annotation class A2(val s: String, val i: Int)

@A0
@A1("A1 on C")
@A2("A2 on C", 1)
class C {
    @A0
    @A1("A1 on C.f")
    @A2("A2 on C.f", 2)
    fun f() {}

    @A0
    @A1("A1 on C.v")
    @A2("A2 on C.v", 3)
    val v = Double.NaN
}
