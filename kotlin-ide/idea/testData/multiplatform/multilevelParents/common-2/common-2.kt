@file:Suppress("UNUSED_PARAMETER")

package sample

expect interface A_Common {
    fun common_1_A()
    fun common_2_A()
}

actual typealias A = A_Common

actual interface B : A {
    actual fun common_1_B()
    fun common_1_2_B()
}

fun takeOutA_common_2(t: Out<A>) {}
fun takeOutB_common_2(t: Out<B>) {}
fun takeOutA_Common_common_2(t: Out<A_Common>) {}

fun getOutA(): Out<A> = null!!
fun getOutB(): Out<B> = null!!
fun getOutA_Common(): Out<A_Common> = null!!

fun test_case_2(x: B) {
    x.common_1_A()
    x.common_1_B()
    x.common_2_A()
    x.common_1_2_B()
}

fun test_B() {
    val x = getB()
    x.common_1_A()
    x.common_1_B()
    x.common_2_A()
    x.common_1_2_B()
}