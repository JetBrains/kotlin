package org.jetbrains

import kotlin.Any

fun foo(p: Int??) {

}

interface T {
    abstract fun foo()
}

fun main(args : Array<String>) {
    System.out?.println(getGreeting())
}

fun getGreeting() : String {
    return "Hello, World!"
}