package app

import lib.*

fun runAppAndReturnOk(): String {
    val i = getInterface()
    val value = i.getInt()
    if (value != 10) error("getInterface().getInt() is '$value', but is expected to be '10'")

    return "OK"
}