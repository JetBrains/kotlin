// FULL_JDK

@Repeatable
annotation class C(val c: String)

@JvmRepeatable(DContainer::class)
annotation class D(val d: String)

annotation class DContainer(val value: Array<D>)

@C(c = "c1")
@D(d = "d1")
@C(c = "c2")
@D(d = "d2")
class A
