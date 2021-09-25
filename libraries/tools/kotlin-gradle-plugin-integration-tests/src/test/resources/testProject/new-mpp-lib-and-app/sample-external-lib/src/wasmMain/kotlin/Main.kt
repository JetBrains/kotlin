package com.example.externalLib

fun idUsage() = id("123")

actual fun expectedFun() = Unit

fun main(args: Array<String>) {
	expectedFun()
}
