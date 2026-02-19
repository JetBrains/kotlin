package app

import lib.*

fun runAppAndReturnOk(): String {
    return Outer.Public.result()
}
