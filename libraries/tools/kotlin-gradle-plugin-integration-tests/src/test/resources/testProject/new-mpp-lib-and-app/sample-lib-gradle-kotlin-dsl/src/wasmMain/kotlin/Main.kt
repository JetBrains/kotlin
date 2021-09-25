package com.example.lib

fun idUsage() = id("123")

actual fun expectedFun() {
}

fun main(args: Array<String>) {
	expectedFun()
}