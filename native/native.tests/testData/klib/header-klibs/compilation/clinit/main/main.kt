package app

import lib.*

fun runAppAndReturnOk(): String {
    if (Object.z != 3) error("lib.Object.z is ${Object.z}, but '3' was expected")
    if (z != 3) error("lib.z is $z, but '3' was expected")

    return "OK"
}