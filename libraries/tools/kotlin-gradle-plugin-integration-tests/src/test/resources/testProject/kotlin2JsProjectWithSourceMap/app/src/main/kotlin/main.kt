@file:Suppress("PackageDirectoryMismatch")

package app

import lib.foo

class C {
    fun somewhereOverTheRainbow() = "🌈"
}

fun main(args: Array<String>) {
    println(C().somewhereOverTheRainbow())
    println(foo())
}
