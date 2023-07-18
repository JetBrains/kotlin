// MODULE: lib
import org.jetbrains.kotlin.fir.plugin.AllOpen

@AllOpen
annotation class Open1

@Open1
annotation class Open2

@Open2
annotation class Open3

// MODULE: main(lib)

import org.jetbrains.kotlin.fir.plugin.AllOpen

@AllOpen
class Zero

@Open1
class First

@Open2
class Second

@Open3
class Third

fun box(): String {
    val a = object : Zero() {}
    val b = object : First() {}
    val c = object : Second() {}
    val d = object : Third() {}
    return "OK"
}
