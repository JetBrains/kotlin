package common.tvos.lib

expect fun platform(): String

fun libFunction() {
    println(platform())
}