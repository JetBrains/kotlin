package app

import lib.*

fun runAppAndReturnOk(): String {
    f1()
    if (v1 != "OK") return "Fail 1"
    f2()
    if (v2 != "OK") return "Fail 2"

    return "OK"
}
