package test

typealias OtherInt = Int

<warning descr="SSR">fun foo1(x: OtherInt) { print(x) }</warning>

fun bar1(x: Int) { print(x) }
fun bar2(x: String) { print(x) }