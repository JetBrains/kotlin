// WITH_STDLIB

annotation class NoArg

open class Base1
open class Base2(val a: Int = 1)
open class Base3(val a: Int) {
    @JvmOverloads constructor(a: Long = 1L) : this(a.toInt())
}

@NoArg class Test1(val b: String) : Base1()
@NoArg class Test2(val b: String) : Base2()
@NoArg class Test3(val b: String) : Base3()

fun box(): String {
    val test1 = Test1::class.java.newInstance()
    val test2 = Test2::class.java.newInstance()
    if (test2.a != 1) return "fail@test2: ${test2.a}"
    val test3 = Test3::class.java.newInstance()
    if (test3.a != 1) return "fail@test3: ${test3.a}"
    return "OK"
}
