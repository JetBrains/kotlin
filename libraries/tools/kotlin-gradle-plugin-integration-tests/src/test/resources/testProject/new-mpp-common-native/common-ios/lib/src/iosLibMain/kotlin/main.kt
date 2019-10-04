package common.ios.lib

expect fun platform(): String

fun libFunction() {
    println(platform())
}