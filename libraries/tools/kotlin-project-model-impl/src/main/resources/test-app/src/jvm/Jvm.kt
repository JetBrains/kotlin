package test.app

fun jvm() = test.lib.xxx() + "APP"

fun main() {
    println(jvm())
}