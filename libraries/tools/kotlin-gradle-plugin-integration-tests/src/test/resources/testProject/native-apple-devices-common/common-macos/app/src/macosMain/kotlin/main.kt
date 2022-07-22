

package common.macos.app

import common.macos.lib.*

expect fun platform(): String

fun appFunction() {
    libFunction()
    println(platform())
}