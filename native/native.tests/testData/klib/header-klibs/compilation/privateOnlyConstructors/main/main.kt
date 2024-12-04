package app

import lib.*

fun runAppAndReturnOk(): String {
    val a = A.create(10)
    if (a.x != 20) error("a.x is '${a.x}', but is expected to be '20'")

    return "OK"
}