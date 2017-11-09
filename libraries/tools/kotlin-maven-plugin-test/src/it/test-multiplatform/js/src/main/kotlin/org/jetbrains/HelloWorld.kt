package org.jetbrains

fun main(args: Array<String>) {
    doMain()
}

actual fun doMain() {
    console.info(getGreeting())
}
