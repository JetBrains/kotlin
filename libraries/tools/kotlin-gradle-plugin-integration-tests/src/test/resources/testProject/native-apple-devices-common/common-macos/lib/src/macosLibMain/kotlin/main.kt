package common.macos.lib

expect fun platform(): String

fun libFunction() {
    println(platform())
}