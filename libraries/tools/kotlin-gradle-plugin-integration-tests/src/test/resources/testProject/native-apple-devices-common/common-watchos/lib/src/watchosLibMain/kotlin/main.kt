package common.watchos.lib

expect fun platform(): String

fun libFunction() {
    println(platform())
}