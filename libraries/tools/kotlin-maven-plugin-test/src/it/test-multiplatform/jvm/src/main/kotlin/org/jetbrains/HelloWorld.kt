package org.jetbrains

fun main(args : Array<String>) {
    doMain()
}

actual fun doMain() {
    System.out?.println(getGreeting())
}