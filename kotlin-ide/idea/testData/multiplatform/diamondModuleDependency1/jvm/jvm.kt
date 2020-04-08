package sample

interface B : A

fun testA(x: A) {
    x.foo()
    x.bar()
    x.<!UNRESOLVED_REFERENCE("baz")!>baz<!>()

    take_A_common_1(x)
    take_A_common_2_1(x)
    take_A_common_2_2(x)
}

fun testB(x: B) {
    x.foo()
    x.bar()
    x.<!UNRESOLVED_REFERENCE("baz")!>baz<!>()

    take_A_common_1(x)
    take_A_common_2_1(x)
    take_A_common_2_2(x) // here we should have some error
}