package app

import lib.*

fun runAppAndReturnOk(): String {
    return inlineFunctionTakingSam { "OK" }
}
