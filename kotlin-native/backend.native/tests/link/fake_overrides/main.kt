import serialization.fake_overrides.*
fun test1() = println(Z().bar())

fun main() {
    test0()
    test1()
    test2()
    test3()
}
