

package common.tvos.app

import common.tvos.lib.*

expect fun platform(): String

fun appFunction() {
    libFunction()
    println(platform())
}