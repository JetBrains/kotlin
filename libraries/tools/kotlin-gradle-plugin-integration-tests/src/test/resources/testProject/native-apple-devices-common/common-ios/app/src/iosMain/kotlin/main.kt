package common.ios.app

import common.ios.lib.*

expect fun platform(): String

fun appFunction() {
    libFunction()
    println(platform())
}