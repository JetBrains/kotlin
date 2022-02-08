package app

import lib.*

fun runAppAndReturnOk(): String {
    foo {
        "OK"
    }
    return result
}
