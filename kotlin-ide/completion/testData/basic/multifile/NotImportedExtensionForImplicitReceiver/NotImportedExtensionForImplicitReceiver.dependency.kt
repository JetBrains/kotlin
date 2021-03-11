package second

import first.C

fun String.helloFun() { }

fun String.helloWithParams(i : Int): String = ""

val String.helloProp: Int get() = 1

val C.helloForC: Int get() = 1

fun Int.helloFake() { }
