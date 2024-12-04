package com.example.lib

fun <T> id(t: T): T = t

expect fun expectedFun(): Unit

fun main() {
    println(">>> Common.kt >>> main()")
}