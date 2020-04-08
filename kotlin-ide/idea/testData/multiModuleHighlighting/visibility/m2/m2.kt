package m2

import m1.*

fun testVisibility() {
    PublicClassInM1()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'InternalClassInM1': it is internal in 'm1'">InternalClassInM1</error>()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'PrivateClassInM1': it is private in file">PrivateClassInM1</error>()

    publicFunInM1()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'internalFunInM1': it is internal in 'm1'">internalFunInM1</error>()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateFunInM1': it is private in file">privateFunInM1</error>()
}

public class ClassInM2

public class B: <error descr="[INVISIBLE_MEMBER] Cannot access '<init>': it is internal in 'A'">A</error>() {

    fun accessA(<warning descr="[UNUSED_PARAMETER] Parameter 'a' is never used">a</warning>: A) {}

    fun f() {
        <error descr="[INVISIBLE_MEMBER] Cannot access 'pri': it is invisible (private in a supertype) in 'B'">pri</error>()

        pro()

        pub()

        <error descr="[INVISIBLE_MEMBER] Cannot access 'int': it is invisible (private in a supertype) in 'B'">int</error>()
    }
}
