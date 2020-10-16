package common.watchos.app

import common.watchos.lib.*

expect fun platform(): String

fun appFunction() {
    libFunction()
    println(platform())
}